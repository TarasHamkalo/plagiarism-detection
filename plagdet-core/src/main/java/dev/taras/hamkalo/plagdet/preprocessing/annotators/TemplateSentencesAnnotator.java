package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.blocks.*;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy.LengthBasedFilteringStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy.RecursiveClusteringExtensionStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationSerializer;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateSentencesAnnotator implements Annotator {

  public static final String TEMPLATE_RESOURCE = "template.txt";

  public static final IndexBasedWordAlignmentSeedingStrategyParameters SEEDING_STRATEGY_PARAMETERS =
    IndexBasedWordAlignmentSeedingStrategyParameters.builder()
      .alignmentSimilarityThreshold(0.9f)
      .vectorSimilarityThreshold(0.8f)
      .build();

  public static final RecursiveClusteringExtensionStrategyParameters EXTENSION_STRATEGY_PARAMETERS =
    RecursiveClusteringExtensionStrategyParameters.builder()
      .minClusterSize(5)
      .sentMaxGap(3)
      .build();

  public static final LengthBasedFilteringStrategyParameters FILTERING_STRATEGY_PARAMETERS =
    LengthBasedFilteringStrategyParameters.builder()
      .minCharacterLength(50)
      .build();

  @NotNull Annotation templateAnnotation;

  @NotNull Embeddings embeddings;

  @NotNull SentenceIndex templateSentenceIndex;

  @NotNull List<Pair<INDArray, INDArray>> templateSentenceMatrices;

  @NotNull SeedingStrategy seedingStrategy;

  @NotNull ExtensionStrategy extensionStrategy;

  @NotNull FilteringStrategy filteringStrategy;

  public TemplateSentencesAnnotator(
    @NotNull Embeddings embeddings,
    @Nullable Annotation templateAnnotation
  ) {
    this.templateAnnotation =
      templateAnnotation == null ? loadTemplateAnnotation() : templateAnnotation;
    this.embeddings = embeddings;
    prepareResources();
  }

  protected void prepareResources() {
    try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().scopeOutOfWorkspaces()) {
      this.templateSentenceIndex = SentenceIndex.from(embeddings, templateAnnotation);
      this.templateSentenceMatrices = new ArrayList<>(
        AnnotationUtils.sentences(templateAnnotation).stream()
          .map(embeddings::getWordVectorsWithWeights)
          .toList()
      );

      this.seedingStrategy = new IndexBasedWordAlignmentSeedingStrategy(
        embeddings, SEEDING_STRATEGY_PARAMETERS
      );
      this.extensionStrategy = new RecursiveClusteringExtensionStrategy(
        embeddings, EXTENSION_STRATEGY_PARAMETERS
      );
      this.filteringStrategy = new LengthBasedFilteringStrategy(FILTERING_STRATEGY_PARAMETERS);
    }

  }

  private @NotNull Annotation loadTemplateAnnotation() {
    var content = IoUtils.resourceContent(TEMPLATE_RESOURCE);
    var annotation = AnnotationSerializer.deserialize(content);
    annotation.remove(CoreAnnotations.TextAnnotation.class);
    return annotation;
  }

  @Override
  public void annotate(Annotation annotation) {
    List<CoreMap> suspSentences = AnnotationUtils.sentences(templateAnnotation);
    List<CoreMap> sourceSentences = AnnotationUtils.sentences(annotation);
    List<List<CoreMap>> documents = List.of(suspSentences, sourceSentences);

    List<int[]> seeds =
      seedingStrategy.seed(templateSentenceIndex, templateSentenceMatrices, sourceSentences);

    List<Fragment> fragments =
      extensionStrategy.extend(documents, seeds);
    List<Fragment> filtered = filteringStrategy.filter(documents, fragments);
    if (filtered.isEmpty()) {
      annotation.set(PlagdetAnnotations.TemplateSentencesAnnotation.class, Collections.emptyList());
      return;
    }

    filtered.sort(
      Comparator.comparingInt((Fragment f) -> f.getSideStarts()[1])
        .thenComparingInt((Fragment f) -> f.getSideEnds()[1])
    );

    assignTextBoundaries(annotation, sourceSentences, filtered);
  }

  private static void assignTextBoundaries(
    @NotNull Annotation annotation,
    List<CoreMap> sourceSentences,
    List<Fragment> filtered
  ) {
    Set<Integer> textBlockBoundaries = new HashSet<>();
    List<int[]> templateOffsets = new ArrayList<>();
    List<CoreMap> filteredSentences = new ArrayList<>(sourceSentences.size());
    int sourceSentencesSize = sourceSentences.size();
    int fragIndex = 0;
    int fragStart = filtered.get(0).getSideStarts()[1];
    int fragEnd = filtered.get(0).getSideEnds()[1];
    int textBlockBegin = 0;
    int beginOffset = 0;
    for (int i = 0; i < sourceSentencesSize; i++) {
      while (i > fragEnd && fragIndex < filtered.size()) {
        fragStart = filtered.get(fragIndex).getSideStarts()[1];
        fragEnd = filtered.get(fragIndex).getSideEnds()[1];
        fragIndex++;
      }

      if (i < fragStart || i > fragEnd) {
        textBlockBegin = filteredSentences.size();
        filteredSentences.add(sourceSentences.get(i));
        continue;
      }

      if (i == fragStart) {
        beginOffset = AnnotationUtils.begin(sourceSentences.get(i));
      }

      if (i == fragEnd) {
        textBlockBoundaries.add(textBlockBegin);
        templateOffsets.add(new int[]{
          beginOffset,
          AnnotationUtils.end(sourceSentences.get(i))
        });
      }

    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, filteredSentences);
    annotation.set(
      CoreAnnotations.TokensAnnotation.class,
      AnnotationUtils.sentences(annotation)
        .stream()
        .flatMap(s -> AnnotationUtils.tokens(s).stream())
        .toList()
    );

    annotation.set(PlagdetAnnotations.TemplateSentencesAnnotation.class, templateOffsets);
    annotation.set(PlagdetAnnotations.TextBlockBoundaries.class, new ArrayList<>(textBlockBoundaries));
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of();
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
