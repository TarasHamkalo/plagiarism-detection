package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.PlagUtil;
import dev.taras.hamkalo.plagdet.comparision.Seed;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.FilteringStrategy;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.ops.transforms.Transforms;

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

    public static final int DEFAULT_MAX_GAP = 2;

    public static final double DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD = 0.8;

    public static final int DEFAULT_MIN_CLUSTER_SIZE = 3;

    public static final double DEFAULT_MIN_FRAGMENT_SIDE_SIM = 0.5;

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
//    public SentenceComparatorParameters() {
//      this(
//        DEFAULT_MAX_GAP,
//        DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD,
//        DEFAULT_MIN_CLUSTER_SIZE,
//        DEFAULT_MIN_FRAGMENT_SIDE_SIM,
//        DEFAULT_MIN_FRAGMENT_CHAR_LENGTH
//      );
//    }
  }

  SentenceComparatorParameters parameters;

  Embeddings embeddings;

  ExtensionStrategy extensionStrategy;

  FilteringStrategy filteringStrategy;

  public SentenceComparator(SentenceComparatorParameters parameters, Embeddings embeddings) {
    this.parameters = parameters;
    this.embeddings = embeddings;
    this.extensionStrategy = new ExtensionStrategy(
      embeddings, parameters.minFragmentSideSim, parameters.minClusterSize
    );
    this.filteringStrategy = new FilteringStrategy(embeddings, parameters.minFragmentCharLength);
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    var fragments = doCompare(documentPair);
    double score = 0;
    if (!fragments.isEmpty()) {
      documentPair.setPlagCases(
        fragments.stream().map(f -> f.toMarkingWithType(SpecialMarkingType.PLAG))
          .collect(Collectors.toList())
      );

      score = PlagUtil.plagScore(documentPair);
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
  public void compareWithTemplate(@NotNull DocumentEntry target,
                                  @NotNull DocumentEntry template) {
    final var targetToTemplate = DocumentPair.builder().first(target).second(template).build();
    var fragments = this.doCompare(targetToTemplate);
    log.warn("Template fragments size {}", fragments.size());

    fragments.stream().flatMap(f -> f.getSeeds().stream())
      .forEach(seed -> {
        seed.getFirstSent().coreMap().set(PlagdetAnnotations.TemplateAnnotation.class, true);
      });

    var markings = fragments.stream()
      .map(f -> f.toMarkingWithType(SpecialMarkingType.TEMPLATE))
      .toList();

    target.setTemplateMarking(markings);
  }

  private @NotNull List<Fragment> doCompare(@NotNull DocumentPair documentPair) {
    embeddings.getWordVector(""); // TODO: remove, debug purpose
    var seeds = seed(documentPair);
    log.debug("Seeds count: {}", seeds.size());
    var fragments = extensionStrategy.extend(seeds, this.parameters.getMaxGap());
    log.debug("Fragments count: {}", fragments.size());

    var filtered = fragments; //filteringStrategy.filter(fragments);
    return filtered;
  }

  private List<Seed> seed(DocumentPair documentPair) {
    var seeds = new ArrayList<Seed>();

    var firstSents = documentPair.getFirst().getCoreDocument().sentences();
    var secondSents = documentPair.getSecond().getCoreDocument().sentences();
    log.debug("First document contains {} sentences", firstSents.size());
    log.debug("Second document contains {} sentences", secondSents.size());
    log.debug("Possible seeds count {}", firstSents.size() * secondSents.size());

    for (int i = 0; i < firstSents.size(); i++) {
      var firstSent = firstSents.get(i);
      if (!isSentenceValid(firstSent)) {
        continue;
      }

      var firstSentAccVector = embeddings.getAccVector(firstSent);
      for (int j = 0; j < secondSents.size(); j++) {
        var secondSent = secondSents.get(j);
        if (!isSentenceValid(secondSent)) {
          continue;
        }

        var secondSentAccVector = embeddings.getAccVector(secondSent);
        var similarity = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        if (!Double.isNaN(similarity) && similarity > parameters.getSentenceVectorSimilarityThreshold()) {
          seeds.add(Seed.builder()
            .firstSentIndex(i)
            .secondSentIndex(j)
            .firstSent(firstSent)
            .secondSent(secondSent)
            .build()
          );
        }
      }
    }

    return seeds;
  }

  private static boolean isSentenceValid(CoreSentence coreSentence) {
    if (coreSentence.coreMap().get(PlagdetAnnotations.CodeSegmentAnnotation.class)) {
      return false;
    }

    var templateAnnotation = coreSentence.coreMap()
      .get(PlagdetAnnotations.TemplateAnnotation.class);
    return templateAnnotation == null || !templateAnnotation;
  }
}
