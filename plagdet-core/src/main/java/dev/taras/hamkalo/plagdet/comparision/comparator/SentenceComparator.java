package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.ops.NDMath;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.nio.file.Path;
import java.util.*;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SentenceComparator {

  public static final String POS_LEMMA_MODEL_RESOURCE = "models/sk.poslemma.prim-10.0-public-saneword2vec.bin";

  public static final int VECTOR_SIZE = 300;

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

  @NotNull Map<String, INDArray> sentenceToWeightedMeanVector;

  @Nullable List<Fragment> seeds;

  @Nullable DocumentPair documentPair;

  public SentenceComparator(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedMeanVector = new HashMap<>();
    this.seeds = new ArrayList<>();
    this.luceneIndex = luceneIndex;
  }

  private static Word2Vec loadWordVectors() {
    var modelPath = Path.of(
      ClassLoader.getSystemClassLoader().getResource(POS_LEMMA_MODEL_RESOURCE).getPath()
    );

    return WordVectorSerializer.readWord2VecModel(modelPath.toFile());
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

  public void compare(DocumentPair documentPair) {
    word2Vec = loadWordVectors();
    this.documentPair = documentPair;
    seed();
    for (var seed : seeds) {
      System.out.println(seed);
    }
  }

  private void seed() {
    this.seeds = new ArrayList<>();
    var srcSents = documentPair.getFirst().getCoreDocument().annotation()
      .get(CoreAnnotations.SentencesAnnotation.class).stream()
      .filter(s -> !s.get(PlagdetAnnotations.CodeSegmentAnnotation.class))
      .toList();

    var suspSents = documentPair.getSecond().getCoreDocument().annotation()
      .get(CoreAnnotations.SentencesAnnotation.class).stream()
      .filter(s -> !s.get(PlagdetAnnotations.CodeSegmentAnnotation.class))
      .toList();

    for (var srcSent : srcSents) {
      var srcSentMeanVector = getMeanVector(srcSent);
//      log.warn(srcSentMeanVector.shapeInfoToString());
      log.debug(Arrays.toString(srcSentMeanVector.toDoubleVector()));
      for (var suspSent : suspSents) {
        var suspSentMeanVector = getMeanVector(suspSent);
        var similarity = Transforms.cosineSim(srcSentMeanVector, suspSentMeanVector);
        if (!Double.isNaN(similarity) && similarity > 0.85) {
          seeds.add(Fragment.builder()
            .firstSentences(Set.of(srcSent))
            .secondSentences(Set.of(suspSent))
            .build()
          );
        }
//        log.warn(Arrays.toString(suspSentMeanVector.toDoubleVector()));
//        log.warn(
//          "\nSimilarity {}\nSent 1:{}\nSent 2:{}",
//          Transforms.cosineSim(srcSentMeanVector, suspSentMeanVector),
//          srcSent.get(CoreAnnotations.TextAnnotation.class),
//          suspSent.get(CoreAnnotations.TextAnnotation.class)
//        );
      }
    }

  }

  private INDArray getMeanVector(CoreMap sentence) {
    return sentenceToWeightedMeanVector.computeIfAbsent(
      sentence.get(CoreAnnotations.TextAnnotation.class), (key) -> computeMeanVector(sentence));
  }

  private INDArray computeMeanVector(CoreMap sentence) {
    INDArray meanVector = Nd4j.zeros(word2Vec.vectorSize());
    for (var wordToken : sentence.get(PlagdetAnnotations.WordsAnnotation.class)) {
      if (wordToken.get(PlagdetAnnotations.StopWordAnnotation.class)) {
        continue;
      }

      INDArray other = getWordVector(wordToken.tag() + wordToken.lemma());
      if (other != null) {
        log.debug("Word vector is of shape {}", other.shapeInfoToString());
        other = other.mul(posWeights.getOrDefault(wordToken.tag(), 0.0));
        log.debug("Word {}, has idf {}", wordToken.lemma(), luceneIndex.inverseTermFrequency(wordToken.lemma()));
        other = other.mul(luceneIndex.inverseTermFrequency(wordToken.lemma()));
        meanVector = meanVector.add(other);
      }
    }
    return meanVector;
  }

}
