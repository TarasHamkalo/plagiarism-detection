package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.ScoreUtils;
import dev.taras.hamkalo.plagdet.comparision.Seed;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.FilteringStrategy;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.Sets;
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

import static dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils.isInformativeSentence;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SentenceComparator implements DocumentComparator, TemplateComparator {

  @Value
  @Builder
  @AllArgsConstructor
  public static class SentenceComparatorParameters {

    public static final int DEFAULT_MAX_GAP = 2;

    public static final double DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD = 0.65;

    public static final double DEFAULT_DICE_SIMILARITY_THRESHOLD = 0.4;

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

    @Builder.Default
    double diceSimilarityThreshold = DEFAULT_DICE_SIMILARITY_THRESHOLD;
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

//      score = PlagUtil.plagScoreAsCharacterCoverage(documentPair);
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
  public void compareWithTemplate(@NotNull DocumentEntry target,
                                  @NotNull DocumentEntry template) {
    final var targetToTemplate = DocumentPair.builder().first(target).second(template).build();
    var fragments = this.doCompare(targetToTemplate);
    log.warn("Template fragments size {}", fragments.size());

    fragments.stream().flatMap(f -> f.getSeeds().stream())
      .forEach(seed -> AnnotationUtils.addTemplateAnnotation(seed.getFirstSent().coreMap()));

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

    var filtered = fragments;
//    var fragments = filteringStrategy.filter(fragments);
    return filtered;
  }

  private List<Seed> seed(DocumentPair documentPair) {
    var seeds = new ArrayList<Seed>();

    var firstSents = documentPair.getFirst().getCoreDocument().sentences();
    var secondSents = documentPair.getSecond().getCoreDocument().sentences();
    log.debug("First document contains {} sentences", firstSents.size());
    log.debug("Second document contains {} sentences", secondSents.size());
    log.debug("Possible seeds count {}", firstSents.size() * secondSents.size());
    // TODO: possible optimization, calculate dice first, without querying for word vectors
    for (int i = 0; i < firstSents.size(); i++) {
      var firstSent = firstSents.get(i);
      if (!isInformativeSentence(firstSent)) {
        continue;
      }

      var firstSentAccVector = embeddings.getAccVector(firstSent);
      for (int j = 0; j < secondSents.size(); j++) {
        var secondSent = secondSents.get(j);
        if (!isInformativeSentence(secondSent)) {
          continue;
        }

        var secondSentAccVector = embeddings.getAccVector(secondSent);
        var cosineSim = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        var diceSim = diceCoefficient(firstSent, secondSent);
        boolean passCosineSim =
          !Double.isNaN(cosineSim) && cosineSim > parameters.getSentenceVectorSimilarityThreshold();
        boolean passDice = diceSim > parameters.getDiceSimilarityThreshold();
        log.trace("Seed:\n\tS1 {}\n\tS2 {}\n\tCosine {}\n\tDice {}\n\n", firstSent, secondSent, cosineSim, diceSim);
        if (passCosineSim && passDice) {
          addSeed(seeds, firstSent, secondSent, i, j);
        }
      }
    }

    return seeds;
  }

  private static void addSeed(
    @NotNull ArrayList<Seed> seeds,
    @NotNull CoreSentence firstSent,
    @NotNull CoreSentence secondSent,
    int i,
    int j
  ) {
    seeds.add(Seed.builder()
      .firstSentIndex(i)
      .secondSentIndex(j)
      .firstSent(firstSent)
      .secondSent(secondSent)
      .build()
    );
  }


  public static double diceCoefficient(
    @NotNull CoreSentence firstSentence,
    @NotNull CoreSentence secondSentence
  ) {
    var firstLemmaSet = firstSentence.coreMap().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .map(CoreLabel::lemma)
      .collect(Collectors.toSet());
    var secondLemmaSet = secondSentence.coreMap().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .map(CoreLabel::lemma)
      .collect(Collectors.toSet());

    double intersectionSize = Sets.intersection(firstLemmaSet, secondLemmaSet).size() * 1.0;
    return 2 * intersectionSize / (firstLemmaSet.size() + secondLemmaSet.size());
  }
}
