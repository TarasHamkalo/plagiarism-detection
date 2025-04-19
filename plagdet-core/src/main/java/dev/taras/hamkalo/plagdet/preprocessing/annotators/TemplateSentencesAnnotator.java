package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.NopExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.SeedingStrategy;
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
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateSentencesAnnotator implements Annotator {

  public static final String TEMPLATE_RESOURCE = "template.txt";

  public static final IndexBasedWordAlignmentSeedingStrategyParameters SEEDING_STRATEGY_PARAMETERS =
    IndexBasedWordAlignmentSeedingStrategyParameters.builder()
      .alignmentSimilarityThreshold(0.9f)
      .vectorSimilarityThreshold(0.8f)
      .build();

  @NotNull Annotation templateAnnotation;

  @NotNull Embeddings embeddings;

  @NotNull SentenceIndex templateSentenceIndex;

  @NotNull List<Pair<INDArray, INDArray>> templateSentenceMatrices;

  @NotNull SeedingStrategy seedingStrategy;

  @NotNull ExtensionStrategy extensionStrategy;

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
    this.templateSentenceIndex = SentenceIndex.from(embeddings, templateAnnotation);
    this.templateSentenceMatrices = new ArrayList<>(
      AnnotationUtils.sentences(templateAnnotation).stream()
        .map(embeddings::getWordVectorsWithWeights)
        .toList()
    );

    this.seedingStrategy = new IndexBasedWordAlignmentSeedingStrategy(
      embeddings, SEEDING_STRATEGY_PARAMETERS
    );
    this.extensionStrategy = new NopExtensionStrategy();
  }

  private @NotNull Annotation loadTemplateAnnotation() {
    var content = IoUtils.resourceContent(TEMPLATE_RESOURCE);
    return AnnotationSerializer.deserialize(content);
  }

  @Override
  public void annotate(Annotation annotation) {
    List<CoreMap> suspSentences = AnnotationUtils.sentences(templateAnnotation);
    List<CoreMap> sourceSentences = AnnotationUtils.sentences(annotation);

    List<int[]> seeds =
      seedingStrategy.seed(templateSentenceIndex, templateSentenceMatrices, sourceSentences);
    List<Fragment> fragments =
      extensionStrategy.extend(List.of(suspSentences, sourceSentences), seeds);
    if (fragments.isEmpty()) {
      annotation.set(PlagdetAnnotations.TemplateSentencesAnnotation.class, Collections.emptyList());
      return;
    }

    fragments.sort(
      Comparator.comparingInt((Fragment f) -> f.getSideStarts()[1])
        .thenComparingInt((Fragment f) -> f.getSideEnds()[1])
    );

    List<Integer> textBlockBoundaries = new ArrayList<>();
    List<int[]> templateOffsets = new ArrayList<>();
    List<CoreMap> filteredSentences = new ArrayList<>(sourceSentences.size());
    int sourceSentencesSize = sourceSentences.size();
    int fragIndex = 0;
    int fragStart = fragments.get(0).getSideStarts()[1];
    int fragEnd = fragments.get(0).getSideEnds()[1];
    int textBlockBegin = 0;
    for (int i = 0; i < sourceSentencesSize; i++) {
      if (i < fragStart) {
        textBlockBegin = filteredSentences.size();
        filteredSentences.add(sourceSentences.get(i));
        continue;
      }

      if (i == fragStart) {
        textBlockBoundaries.add(textBlockBegin);
      }

      while (i > fragEnd && fragIndex < fragments.size()) {
        fragStart = fragments.get(fragIndex).getSideStarts()[1];
        fragEnd = fragments.get(fragIndex).getSideEnds()[1];
        fragIndex++;
      }

      if (i < fragStart || i > fragEnd) {
        filteredSentences.add(sourceSentences.get(i));
        continue;
      }

      templateOffsets.add(new int[]{
        AnnotationUtils.begin(sourceSentences.get(i)),
        AnnotationUtils.end(sourceSentences.get(i))
      });
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
    annotation.set(PlagdetAnnotations.TextBlockBoundaries.class, textBlockBoundaries);
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
