package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.*;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SentenceComparator implements DocumentComparator {

  public static final String POS_LEMMA_MODEL_RESOURCE = "models/sk.poslemma.prim-10.0-public-saneword2vec.bin";

  public static final int WORD_VECTOR_SIZE = 200;

  public static final int MAX_GAP = 2;

  public static final int MIN_CLUSTER_SIZE = 3;

  public static final int MIN_FRAGMENT_CHAR_LENGTH = 10;

  public static final double MIN_FRAGMENT_SIDE_SIM = 0.5;

  private static final double SENTENCE_VECTOR_SIM_TH = 0.8;

  public static final Map<String, Double> posWeights = Map.ofEntries(
    Map.entry("Nn", 1.0),
    Map.entry("Aj", 1.0),
    Map.entry("Vb", 1.0),
    Map.entry("Pn", 1.0),
    Map.entry("Nm", 1.0),
    Map.entry("Av", 1.0),
    Map.entry("Pp", 1.0),
    Map.entry("Cj", 1.0),
    Map.entry("Pt", 1.0),
    Map.entry("Ab", 1.0),
    Map.entry("Sy", 1.0)
  );

  @Nullable
  private static Word2Vec word2Vec;

  @NotNull
  final LuceneIndex luceneIndex;

  @NotNull
  final Map<String, INDArray> sentenceToWeightedAccVector;

  public SentenceComparator(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedAccVector = new HashMap<>();
    this.luceneIndex = luceneIndex;
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    var fragments = doCompare(documentPair);
    if (!fragments.isEmpty()) {
      documentPair.setPlagCases(
        fragments.stream().map(f -> f.toMarkingWithType(SpecialMarkingType.PLAG))
          .collect(Collectors.toList())
      );

      documentPair.getPlagScores().add(
        PlagScore.builder()
          .score(PlagUtil.plagScore(documentPair))
          .direction(Direction.UNCERTAIN)
          .comments(List.of())
          .build()
      );
    }
    return ComparisonResult.CONTINUE;
  }

  public void compareWithTemplate(@NotNull DocumentEntry target,
                                  @NotNull DocumentEntry template) {
    final var targetToTemplate = DocumentPair.builder().first(target).second(template).build();
    var fragments = this.doCompare(targetToTemplate);
    log.warn("Template framgnets size {}", fragments.size());

    fragments.stream().flatMap(f -> f.getSeeds().stream())
      .forEach(seed -> {
        seed.getFirstSent().coreMap().set(PlagdetAnnotations.TemplateAnnotation.class, true);
      });
//    specialMarking -> specialMarking.withType(SpecialMarkingType.TEMPLATE)
    var markings = fragments.stream()
      .map(f -> f.toMarkingWithType(SpecialMarkingType.TEMPLATE))
      .toList();

    target.setTemplateMarking(markings);
  }

  private @NotNull List<Fragment> doCompare(@NotNull DocumentPair documentPair) {
    getWordVector(""); // TODO: remove, debug purpose
    log.info("Comparing {} - {}", documentPair.getFirst().getFileData(), documentPair.getSecond().getFileData());

    var seeds = seed(documentPair);
    log.debug("Seeds count: {}", seeds.size());
    var fragments = extension(seeds, MAX_GAP);
    log.debug("Fragments count: {}", fragments.size());
    var filtered = fragments; //filtering(fragments);
    return filtered;
  }

  private List<Fragment> filtering(List<Fragment> fragments) {
//    var filteredFragments = removeOverlapping(fragments);
    var filteredFragments = removeShort(fragments);
    return filteredFragments;
  }

  private List<Fragment> removeShort(List<Fragment> fragments) {
    var filteredFragments = new ArrayList<Fragment>();
    for (var fragment : fragments) {
      fragment.sortSeedsBySideIndex(0);
      var firstSideLength =
        fragment.lastSeed().getFirstSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetEndAnnotation.class) -
        fragment.firstSeed().getFirstSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);

      fragment.sortSeedsBySideIndex(1);

      var secondSideLength =
        fragment.lastSeed().getSecondSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetEndAnnotation.class) -
        fragment.firstSeed().getSecondSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);

      if (firstSideLength > MIN_FRAGMENT_CHAR_LENGTH &&
          secondSideLength > MIN_FRAGMENT_CHAR_LENGTH) {
        filteredFragments.add(fragment);
      }
    }

    return filteredFragments;
  }

  private List<Fragment> removeOverlapping(List<Fragment> fragments) {
    // after that sorting, whenever we find non overlapping cluster,
    // there is no need to proceed further
    for (var fragment : fragments) {
      fragment.sortSeedsBySideIndex(0);
    }

    fragments.sort(Comparator.comparingInt(c -> c.getStartSideIndex(0)));
    var filteredFragments = new ArrayList<Fragment>();

    int i = 0;
    while (i < fragments.size()) {
      var source = fragments.get(i);
      int j = i + 1;
      for (; j < fragments.size(); j++) {
        var target = fragments.get(j);
        if (!overlaps(source, target)) {
          break;
        }
      }

      if (j == i + 1) {
        filteredFragments.add(source);
        i++;
      } else {
        int best = pickBestFragment(fragments.subList(i, j));
        filteredFragments.add(fragments.get(i + best));
        i = j;
      }
    }

    return filteredFragments;
  }

  private int pickBestFragment(List<Fragment> overlapping) {
    var pivot = overlapping.get(0);
    int maxi = 0;
    double maxSim = Double.MIN_VALUE;
    for (int i = 1; i < overlapping.size(); i++) {
      var other = overlapping.get(i);
      var overlappedSents = pivot.intersect(other);
      var overlapToPivotComplement = pivot.complement(other);
      var overlapToOtherComplement = other.complement(pivot);

      double pivotFragmentQuality = fragmentQuality(pivot.copy(), overlappedSents, overlapToPivotComplement);
      double otherFragmentQuality = fragmentQuality(other.copy(), overlappedSents, overlapToOtherComplement);
      if (pivotFragmentQuality > otherFragmentQuality) {
        if (pivotFragmentQuality > maxSim) {
          maxi = 0;
          maxSim = pivotFragmentQuality;
        }
      }

      if (otherFragmentQuality > pivotFragmentQuality) {
        if (otherFragmentQuality > maxSim) {
          maxi = i;
          maxSim = otherFragmentQuality;
        }
      }
    }

    return maxi;
  }

  /**
   * Computes fragment quality considering overlapped part "fitness" and complement "fitness"
   *
   * @implNote Fragment will be sorted by second document
   */
  private double fragmentQuality(Fragment fragment,
                                 List<CoreSentence> overlap,
                                 List<CoreSentence> complement) {
    fragment.sortSeedsBySideIndex(1);
    var secondCoreDocument = fragment.getSeeds().get(0).getSecondSent().document();

    int secondStartIndex = fragment.getStartSideIndex(1);
    int secondEndIndex = fragment.getEndSideIndex(1);
    var secondDocumentSentences = secondCoreDocument.sentences()
      .subList(secondStartIndex, secondEndIndex + 1);

    double overlapToFragmentSim = fragmentSimilarityAsymmetric(overlap, secondDocumentSentences);
    double complementToFragmentSim = fragmentSimilarityAsymmetric(complement, secondDocumentSentences);

    return overlapToFragmentSim + (1 - overlapToFragmentSim) * complementToFragmentSim;
  }

  /**
   * Compares first fragment to the second using average bestMatch
   */
  private double fragmentSimilarityAsymmetric(List<CoreSentence> firstFragment,
                                              List<CoreSentence> secondFragment) {

    double bestMatchSum = 0.0;
    for (var firstSent : firstFragment) {
      var firstSentAccVector = getAccVector(firstSent);
      double maxSim = Double.NaN;
      for (var secondSent : secondFragment) {
        var secondSentAccVector = getAccVector(secondSent);
        double sim = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        if (Double.isNaN(maxSim)) {
          maxSim = sim;
        } else if (!Double.isNaN(sim)) {
          maxSim = Math.max(maxSim, sim);
        }
      }

      if (!Double.isNaN(maxSim)) {
        bestMatchSum += maxSim;
      }
    }

    return bestMatchSum / secondFragment.size();
  }

  /**
   * Cluster seeds are expected to be sorted by the first document position
   * Also clusters should be sorted, e.g. target's first document start pos > source start post
   *
   * @return True if source and target clusters overlap on side of first document in document pair
   */
  private boolean overlaps(Fragment source, Fragment target) {
    return source.getEndSideIndex(0) - target.getStartSideIndex(0) > 0;
  }

  private List<Fragment> extension(List<Seed> seeds, int sentMaxGap) {
    var fragments = clustering(seeds, sentMaxGap, 0);
    fragments = validation(fragments, sentMaxGap);
    return fragments;
  }

  /**
   * Split fragment each time it sides have low similarity score
   *
   * @return List of fragment with having similarity score at least MIN_CLUSTER_SIDE_SIM
   */
  private @NotNull List<Fragment> validation(@NotNull List<Fragment> fragments, int sentMaxGap) {
    List<Fragment> newFragments = new ArrayList<>();
    for (var fragment : fragments) {
      var sideSim = sideSimilarity(fragment);
      if (sideSim < MIN_FRAGMENT_SIDE_SIM) {
        log.warn("Sim is {}, so splitting fragment with {} seeds", sideSim, fragment.getSeeds().size());
        if (sentMaxGap > 0) {
          newFragments.addAll(extension(fragment.getSeeds(), sentMaxGap - 1));
        }
      } else {
        newFragments.add(fragment);
      }
    }

    return newFragments;
  }

  private double sideSimilarity(@NotNull Fragment fragment) {
    var firstAccVector = getSideAccVector(fragment, 0);
    var secondAccVector = getSideAccVector(fragment, 1);

    return Transforms.cosineSim(firstAccVector, secondAccVector);
  }

  private @NotNull INDArray getSideAccVector(@NotNull Fragment fragment, int side) {
    var sideAccVector = Nd4j.zeros(WORD_VECTOR_SIZE);
    var seeds = fragment.getSeeds();
    seeds.sort(Comparator.comparingInt(s -> s.getSideIndex(side)));

    var minSentIndex = seeds.get(0).getSideIndex(side);
    var maxSentIndex = seeds.get(seeds.size() - 1).getSideIndex(side);

    var coreDocument = seeds.get(0).getSideSentence(side).document();
    for (var sent : coreDocument.sentences().subList(minSentIndex, maxSentIndex)) {
      var sentAccVector = getAccVector(sent);
      sideAccVector.addi(sentAccVector);
    }

    return sideAccVector;
  }

  /**
   * Merges seeds into continuous fragments, considering only one side of document pair
   *
   * @return list of merged seeds
   */
  private @NotNull List<Fragment> clusterOneSide(@NotNull List<Seed> seeds, int sentMaxGap, int side) {
    seeds.sort(Comparator.comparingInt(s -> s.getSideIndex(side)));

    var clusters = new ArrayList<Fragment>();
    var cluster = new ArrayList<Seed>();
    for (Seed seed : seeds) {
      if (cluster.isEmpty()) {
        cluster.add(seed);
        continue;
      }

      var lastSeed = cluster.get(cluster.size() - 1);
      if (lastSeed.isAdjacent(seed, side, sentMaxGap)) {
        cluster.add(seed);
      } else {
        if (cluster.size() >= MIN_CLUSTER_SIZE) {
          clusters.add(Fragment.builder().seeds(new ArrayList<>(cluster)).build());
        }
        cluster.clear();
        cluster.add(seed);
      }
    }

    if (cluster.size() >= MIN_CLUSTER_SIZE) {
      clusters.add(Fragment.builder().seeds(new ArrayList<>(cluster)).build());
    }

    return clusters;
  }

  private List<Fragment> clustering(List<Seed> seeds, int sentMaxGap, int side) {
    var clusters = clusterOneSide(seeds, sentMaxGap, side);
    if (clusters.size() == 1) {
      return clusters;
    }

    List<Fragment> newClusters = new ArrayList<>();
    for (var cluster : clusters) {
      newClusters.addAll(clustering(cluster.getSeeds(), sentMaxGap, side + 1));
    }

    return newClusters;
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

      var firstSentAccVector = getAccVector(firstSent);
      for (int j = 0; j < secondSents.size(); j++) {
        var secondSent = secondSents.get(j);
        if (!isSentenceValid(secondSent)) {
          continue;
        }

        var secondSentAccVector = getAccVector(secondSent);
        var similarity = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        if (!Double.isNaN(similarity) && similarity > SENTENCE_VECTOR_SIM_TH) {
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

  private @NotNull INDArray getAccVector(@NotNull CoreSentence sentence) {
    return sentenceToWeightedAccVector.computeIfAbsent(
      sentence.text(), (key) -> computeAccVector(sentence.coreMap()));
  }

  private @NotNull INDArray computeAccVector(@NotNull CoreMap sentence) {
    INDArray accVector = Nd4j.zeros(SentenceComparator.WORD_VECTOR_SIZE);
    for (var wordToken : sentence.get(PlagdetAnnotations.WordsAnnotation.class)) {
      if (wordToken.get(PlagdetAnnotations.StopWordAnnotation.class)) {
        continue;
      }

      INDArray other = getWordVector(wordToken.tag() + wordToken.lemma());
      if (other != null) {
        log.trace("Word vector is of shape {}", other.shapeInfoToString());
        other.muli(posWeights.getOrDefault(wordToken.tag(), 0.0));
        log.trace(
          "Word {}, has idf {}", wordToken.lemma(),
          luceneIndex.inverseTermFrequency(wordToken.lemma())
        );
        other.muli(luceneIndex.inverseTermFrequency(wordToken.lemma()));
        accVector.addi(other);
      }
    }

    return accVector;
  }

  public @Nullable INDArray getWordVector(@NotNull String word) {
    if (word2Vec == null) {
      word2Vec = loadWordVectors();
    }

    var wordVec = word2Vec.getWordVector(word);
    if (wordVec != null) {
      return Nd4j.createFromArray(wordVec);
    }

    return null;
  }

  private static @Nullable Word2Vec loadWordVectors() {
    var modelPath = Path.of(
      ClassLoader.getSystemClassLoader().getResource(POS_LEMMA_MODEL_RESOURCE).getPath()
    );
    log.warn("Loading Word2Vec model from {}", modelPath);
    var word2vec = WordVectorSerializer.readWord2VecModel(modelPath.toFile());
    log.warn("Loading Word2Vec model done");
    return word2vec;
  }

}
