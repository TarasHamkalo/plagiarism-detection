package dev.taras.hamkalo.plagdet.comparision.comparator.blocks;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
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

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Embeddings {

  public static final String MODEL_RESOURCE_PREFIX = "models/";

  public static final String POS_LEMMA_MODEL =
    "sk.poslemma.prim-10.0-public-saneword2vec.bin";

  public static final int WORD_VECTOR_SIZE = 200;

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
  private static volatile Word2Vec word2Vec;

  @NotNull
  LuceneIndex luceneIndex;

  @NotNull
  Map<String, INDArray> sentenceToWeightedAccVector;

  public Embeddings(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedAccVector = new ConcurrentHashMap<>();
    this.luceneIndex = luceneIndex;
  }

  public @NotNull INDArray getAccVector(@NotNull CoreSentence sentence) {
    return sentenceToWeightedAccVector.computeIfAbsent(
      sentence.text(), (key) -> computeAccVector(sentence.coreMap()));
  }

  public @Nullable INDArray getWordVector(@NotNull String word) {
    if (word2Vec == null) {
      synchronized (Embeddings.class) {
        if (word2Vec == null) {
          word2Vec = loadWordVectors();
        }
      }
    }

    var wordVec = word2Vec.getWordVector(word);
    if (wordVec != null) {
      return Nd4j.createFromArray(wordVec);
    }

    return null;
  }

  public @NotNull INDArray getSideAccVector(@NotNull Fragment fragment, int side) {
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

  private @NotNull INDArray computeAccVector(@NotNull CoreMap sentence) {
    INDArray accVector = Nd4j.zeros(Embeddings.WORD_VECTOR_SIZE);
    for (var wordToken : AnnotationUtils.wordTokens(sentence)) {
      if (wordToken.get(PlagdetAnnotations.StopWordAnnotation.class)) {
        continue;
      }

      INDArray other = getWordVector(wordToken.tag() + wordToken.lemma());
      if (other != null) {
        log.trace("Word vector is of shape {}", other.shapeInfoToString());
        other.muli(posWeights.getOrDefault(wordToken.tag(), 0.0));
        double idf = 1;
        try {
          idf = luceneIndex.inverseTermFrequency(wordToken.lemma());
        } catch (IndexingException ignoreEmptyIndex) {
        }

        log.trace("Word {}, has idf {}", wordToken.lemma(), idf);
        log.trace("Using idf {}", idf);
        other.muli(idf);
        accVector.addi(other);
      }
    }

    return accVector;
  }

  private static @Nullable Word2Vec loadWordVectors() {
    var modelPath = IoUtils.resourceFromTemp(MODEL_RESOURCE_PREFIX + POS_LEMMA_MODEL, POS_LEMMA_MODEL);
    log.warn("Loading Word2Vec model from {}", MODEL_RESOURCE_PREFIX + POS_LEMMA_MODEL);
    var word2vec = WordVectorSerializer.readWord2VecModel(modelPath.toFile());
    log.warn("Loading Word2Vec model done");
    return word2vec;
  }

}
