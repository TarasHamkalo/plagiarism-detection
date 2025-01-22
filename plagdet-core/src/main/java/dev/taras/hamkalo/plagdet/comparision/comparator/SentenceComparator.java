package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Cluster;
import dev.taras.hamkalo.plagdet.comparision.Seed;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
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

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SentenceComparator {

  public static final String POS_LEMMA_MODEL_RESOURCE = "models/sk.poslemma.prim-10.0-public-saneword2vec.bin";

  public static final int WORD_VECTOR_SIZE = 200;

  public static final int MAX_GAP = 1;

  public static final int MIN_CLUSTER_SIZE = 3;

  private static final double MIN_CLUSTER_SIDE_SIM = 0.8;

  public static final Map<String, Double> posWeights = Map.ofEntries(
    Map.entry("Nn", 0.5),
    Map.entry("Aj", 0.3),
    Map.entry("Vb", 0.4),
    Map.entry("Pn", 0.2),
    Map.entry("Nm", 0.0),
    Map.entry("Av", 0.3),
    Map.entry("Pp", 0.0),
    Map.entry("Cj", 0.0),
    Map.entry("Pt", 0.0),
    Map.entry("Ab", 0.4),
    Map.entry("Sy", 0.0)
  );

  @Nullable Word2Vec word2Vec;

  @NotNull
  final LuceneIndex luceneIndex;

  @NotNull
  final Map<String, INDArray> sentenceToWeightedAccVector;

  public SentenceComparator(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedAccVector = new HashMap<>();
    this.luceneIndex = luceneIndex;
  }

  public void compare(@NotNull DocumentPair documentPair) {
    var seeds = seed(documentPair);
    var clusters = extension(seeds, MAX_GAP);
    var filtered = filtering(clusters);

    System.out.println(clusters.size());
    System.out.println(filtered.size());
//    clusterOneSide(seeds, 2, 0);
//    extension(seeds);
  }

  private List<Cluster> filtering(List<Cluster> clusters) {
    List<Cluster> filteredClusters = new ArrayList<>();

    // after that sorting, whenever we find non overlapping cluster,
    // there is no need to proceed further
    for (var cluster : clusters) {
      cluster.sortSeedsBySideIndex(0);
    }
    clusters.sort(Comparator.comparingInt(c -> c.getStartSideIndex(0)));

    int i = 0;
    for (; i < clusters.size();) {
      var source = clusters.get(i);
      int j = i + 1;
      for (; j < clusters.size(); j++) {
        var target = clusters.get(j);
        if (!overlap(source, target)) {
          break;
        }
      }

      if (j == i + 1) {
        filteredClusters.add(source);
        i++;
      } else {
        int best = pickBestCluster(clusters.subList(i, j));
        filteredClusters.add(clusters.get(i + best));
        i = j;
      }

    }

    return filteredClusters;
  }

  private int pickBestCluster(List<Cluster> overlapping) {
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
   * Computes cluster quality considering overlapped part "fitness" and complement "fitness"
   * @implNote Cluster should be sorted by second document
   */
  private double fragmentQuality(Cluster cluster,
                                 List<CoreSentence> overlap,
                                 List<CoreSentence> complement) {
    var secondCoreDocument = cluster.getSeeds().getFirst().getSecondSent().document();
    cluster.sortSeedsBySideIndex(1);

    int secondStartIndex = cluster.getStartSideIndex(1);
    int secondEndIndex = cluster.getEndSideIndex(1);
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

      if (Double.isNaN(maxSim)) {
        bestMatchSum += maxSim;
      }
    }

    return bestMatchSum / secondFragment.size();
  }

  /**
   * Cluster seeds are expected to be sorted by the first document position
   * Also clusters should be sorted, e.g. target's first document start pos > source start post
   * @return True if source and target clusters overlap on side of first document in document pair
   */
  private boolean overlap(Cluster source, Cluster target) {
    return source.getEndSideIndex(0) - target.getStartSideIndex(0) > 0;
  }

  private List<Cluster> extension(List<Seed> seeds, int sentMaxGap) {
    var clusters = clustering(seeds, sentMaxGap, 0);
    clusters = validation(clusters, sentMaxGap);
    return clusters;
  }

  /**
   * Split cluster each time it sides have low similarity score
   * @return List of clusters with having similarity score at least MIN_CLUSTER_SIDE_SIM
   */
  private @NotNull List<Cluster> validation(@NotNull List<Cluster> clusters, int sentMaxGap) {
    List<Cluster> newClusters = new ArrayList<>();
    for (var cluster : clusters) {
      var sideSim = sideSimilarity(cluster);
      if (sideSim < MIN_CLUSTER_SIDE_SIM) {
        log.warn("Sim is {}, so splitting cluster with {} seeds", sideSim, cluster.getSeeds().size());
        if (sentMaxGap > 0) {
          newClusters.addAll(extension(cluster.getSeeds(), sentMaxGap - 1));
        }
      } else {
        newClusters.add(cluster);
      }
    }
    return clusters;
  }

  private double sideSimilarity(@NotNull Cluster cluster) {
    var firstAccVector = getSideAccVector(cluster, 0);
    var secondAccVector = getSideAccVector(cluster, 0);

    return Transforms.cosineSim(firstAccVector, secondAccVector);
  }

  private @NotNull INDArray getSideAccVector(@NotNull Cluster cluster, int side) {
    var sideAccVector = Nd4j.zeros(WORD_VECTOR_SIZE);
    var seeds = cluster.getSeeds();
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
  private @NotNull List<Cluster> clusterOneSide(@NotNull List<Seed> seeds, int sentMaxGap, int side) {
    seeds.sort(Comparator.comparingInt(s -> s.getSideIndex(side)));

    var clusters = new ArrayList<Cluster>();
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
          clusters.add(Cluster.builder().seeds(new ArrayList<>(cluster)).build());
        }
        cluster.clear();
        cluster.add(seed);
      }
    }

    if (cluster.size() >= MIN_CLUSTER_SIZE) {
      clusters.add(Cluster.builder().seeds(new ArrayList<>(cluster)).build());
    }

    return clusters;
  }

  private List<Cluster> clustering(List<Seed> seeds, int sentMaxGap, int side) {
    var clusters = clusterOneSide(seeds, sentMaxGap, side);
    if (clusters.size() == 1) {
      return clusters;
    }

    List<Cluster> newClusters = new ArrayList<>();
    for (var cluster : clusters) {
      newClusters.addAll(clustering(cluster.getSeeds(), sentMaxGap, side + 1));
    }

    return newClusters;
  }

  private List<Seed> seed(DocumentPair documentPair) {
    var seeds = new ArrayList<Seed>();

    var firstSents = documentPair.getFirst().getCoreDocument().sentences();
    var secondSents = documentPair.getSecond().getCoreDocument().sentences();

    for (int i = 0; i < firstSents.size(); i++) {
      var firstSent = firstSents.get(i);
      if (firstSent.coreMap().get(PlagdetAnnotations.CodeSegmentAnnotation.class)) {
        continue;
      }

      var firstSentAccVector = getAccVector(firstSent);
      for (int j = 0; j < secondSents.size(); j++) {
        var secondSent = secondSents.get(j);
        if (secondSent.coreMap().get(PlagdetAnnotations.CodeSegmentAnnotation.class)) {
          continue;
        }

        var secondSentAccVector = getAccVector(secondSent);
        var similarity = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        if (!Double.isNaN(similarity) && similarity > 0.85) {
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
        log.debug("Word vector is of shape {}", other.shapeInfoToString());
        other = other.mul(posWeights.getOrDefault(wordToken.tag(), 0.0));
        log.debug(
          "Word {}, has idf {}", wordToken.lemma(),
          luceneIndex.inverseTermFrequency(wordToken.lemma())
        );
        other = other.mul(luceneIndex.inverseTermFrequency(wordToken.lemma()));
        accVector = accVector.add(other);
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
