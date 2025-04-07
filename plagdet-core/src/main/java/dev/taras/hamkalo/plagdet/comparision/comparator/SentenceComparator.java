package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.NopExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.seeding.*;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.util.ScoreUtils;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SentenceComparator implements DocumentComparator, TemplateComparator {

  @Value
  @Builder
  @AllArgsConstructor
  public static class SentenceComparatorParameters {

    public static final int DEFAULT_MAX_GAP = 0;

    public static final double DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD = 0.65;

    public static final double DEFAULT_DICE_SIMILARITY_THRESHOLD = 0.5;

    public static final int DEFAULT_MIN_CLUSTER_SIZE = 1;

    public static final double DEFAULT_MIN_FRAGMENT_SIDE_SIM = 0.7;

    public static final int DEFAULT_MIN_FRAGMENT_CHAR_LENGTH = 150;

    @Builder.Default
    int maxGap = DEFAULT_MAX_GAP;

    @Builder.Default
    double sentenceVectorSimilarityThreshold = DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD;

    @Builder.Default
    int minClusterSize = DEFAULT_MIN_CLUSTER_SIZE;

    @Builder.Default
    double minFragmentSideSim = DEFAULT_MIN_FRAGMENT_SIDE_SIM;

    @Builder.Default
    int minFragmentCharLength = DEFAULT_MIN_FRAGMENT_CHAR_LENGTH;

    @Builder.Default
    double diceSimilarityThreshold = DEFAULT_DICE_SIMILARITY_THRESHOLD;

  }

  SentenceComparatorParameters parameters;

  Embeddings embeddings;

  ExtensionStrategy extensionStrategy;

  SeedingStrategy seedingStrategy;

  public SentenceComparator(SentenceComparatorParameters parameters, Embeddings embeddings) {
    this.parameters = parameters;
    this.embeddings = embeddings;
    this.extensionStrategy = new NopExtensionStrategy(
      embeddings, parameters.minFragmentSideSim, parameters.minClusterSize
    );

//    this.seedingStrategy = new Word2vecCosineDiceSeeding(
//      embeddings, parameters.getDiceSimilarityThreshold(), parameters.getSentenceVectorSimilarityThreshold()
//    );
//
    this.seedingStrategy = new WeightedWordAlignment(
      embeddings, parameters.getSentenceVectorSimilarityThreshold()
    );

//    this.seedingStrategy = new HybridSeedingStrategy(
//      embeddings, parameters.getSentenceVectorSimilarityThreshold()
//    );
//    this.seedingStrategy = new CosineWord2vecWordAlignment(
//      embeddings, parameters.getSentenceVectorSimilarityThreshold(), 0.7
//    );
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    List<List<CoreMap>> documents = new ArrayList<>(List.of(
      AnnotationUtils.sentences(documentPair.getFirst().getAnnotation()),
      AnnotationUtils.sentences(documentPair.getSecond().getAnnotation())
    ));

    var fragments = doCompare(documents);
    double score = 0;
    if (!fragments.isEmpty()) {
      documentPair.setPlagCases(
        fragments.stream()
          .map(f -> f.toMarkingWithType(documents, SpecialMarkingType.PLAG))
          .collect(Collectors.toList())
      );

      score = ScoreUtils.plagScoreAsSentenceCoverage(documentPair, fragments);
    }

    documentPair.getPlagScores().add(
      PlagScore.builder()
        .score(score)
        .type(PlagScore.PlagScoreType.SEMANTIC)
        .build()
    );
    return ComparisonResult.CONTINUE;
  }

  @Override
  public void compareWithTemplate(
    @NotNull DocumentEntry target,
    @NotNull DocumentEntry template
  ) {
    log.error("Comparing to template " + target.getFileData().getPersistentPath());
    List<List<CoreMap>> documents = new ArrayList<>(List.of(
      AnnotationUtils.sentences(target.getAnnotation()),
      AnnotationUtils.sentences(template.getAnnotation())
    ));

    var fragments = doCompare(documents);
    log.trace("Template fragments size {}", fragments.size());
    var seeds = fragments.stream().flatMap(f -> f.getSeeds(0).stream()).map(s -> s[0]).toList();
    var markings = fragments.stream()
      .map(f -> f.toMarkingWithType(documents, SpecialMarkingType.TEMPLATE))
      .toList();

    AnnotationUtils.removeTemplateSentences(target.getAnnotation(), seeds);


    target.setComparedToTemplate(true);
    target.setTemplateMarking(markings);
  }

  private @NotNull List<Fragment> doCompare(@NotNull List<List<CoreMap>> documents) {
    var seeds = this.seedingStrategy.seed(documents);
    log.trace("Seeds count: {}", seeds.size());
    var fragments = extensionStrategy.extend(documents, seeds, this.parameters.getMaxGap());
    log.trace("Fragments count: {}", fragments.size());

    var filtered = fragments;
//    var fragments = filteringStrategy.filter(fragments);
    return filtered;
  }


}
