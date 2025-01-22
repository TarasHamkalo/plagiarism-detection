package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
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

  public static final int VECTOR_SIZE = 200;

  public static final int MAX_GAP = 2;

  public static final int MIN_CLUSTER_SIZE = 3;

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

  @NotNull LuceneIndex luceneIndex;

  @NotNull Map<String, INDArray> sentenceToWeightedAccVector;

  public SentenceComparator(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedAccVector = new HashMap<>();
    this.luceneIndex = luceneIndex;
  }

  public void compare(@NotNull DocumentPair documentPair) {
    var seeds = seed(documentPair);
    mergeSeeds(seeds, 2, 0);
//    extension(seeds);
  }

  private void extension(List<Seed> fragments) {
    var clusters = clustering(fragments);
  }

  /**
   * Merges seeds into continuous fragments, considering only one side of document pair
   *
   * @return list of merged seeds
   */
  private List<Fragment> mergeSeeds(List<Seed> seeds, int sentMaxGap, int side) {
    seeds.sort(Comparator.comparingInt(s -> s.getSideOffset(side)));

    var fragments = new ArrayList<Fragment>();
    var merged = new ArrayList<Seed>();
    for (Seed seed : seeds) {
      if (merged.isEmpty()) {
        merged.add(seed);
        continue;
      }

      var lastSeed = merged.get(merged.size() - 1);
      if (lastSeed.isAdjacent(seed, side, sentMaxGap)) {
        merged.add(seed);
      } else {
        if (merged.size() >= MIN_CLUSTER_SIZE) {
          fragments.add(Fragment.builder().seeds(new ArrayList<>(merged)).build());
        }
        merged.clear();
        merged.add(seed);
      }
    }

    if (merged.size() >= MIN_CLUSTER_SIZE) {
      fragments.add(Fragment.builder().seeds(new ArrayList<>(merged)).build());
    }

    return fragments;
  }

  private List<Fragment> clustering(List<Seed> seeds) {
    return null;
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
    INDArray accVector = Nd4j.zeros(SentenceComparator.VECTOR_SIZE);
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

    return WordVectorSerializer.readWord2VecModel(modelPath.toFile());
  }

}
