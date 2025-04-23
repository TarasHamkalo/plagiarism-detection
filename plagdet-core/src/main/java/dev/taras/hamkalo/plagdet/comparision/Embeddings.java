package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.indexing.exception.LuceneException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Embeddings {

  public static final String MODEL_RESOURCE_PREFIX = "models/";

  public static final String POS_LEMMA_MODEL =
    "sk.poslemma.prim-10.0-public-saneword2vec.bin";

  public static final int WORD_VECTOR_SIZE = 200;

  public static final Map<String, Double> posWeights = Map.ofEntries(
    Map.entry("Nn", 1.2),  // Nouns are more important
    Map.entry("Aj", 1.1),  // Adjectives contribute meaning
    Map.entry("Vb", 1.3),  // Verbs carry significant action
    Map.entry("Pn", 0.9),  // Pronouns are less relevant
    Map.entry("Nm", 0.0),  // Numerals are already removed
    Map.entry("Av", 1.0),  // Adverbs are neutral
    Map.entry("Pp", 0.8),  // Prepositions have lower weight
    Map.entry("Cj", 0.7),  // Conjunctions are less relevant
    Map.entry("Pt", 0.6),  // Particles add minor meaning
    Map.entry("Ab", 0.5),  // Abbreviations are least relevant
    Map.entry("Sy", 0.3)   // Symbols are the least important
  );

  @Nullable
  @NonFinal
  Word2Vec word2Vec;

  @NotNull
  LuceneIndex luceneIndex;

  @NotNull
  Map<String, INDArray> sentenceToWeightedAccVector; // TODO: clear cache

  @NotNull
  Map<String, INDArray> wordVectorCache;

  public Embeddings(@NotNull LuceneIndex luceneIndex) {
    this.luceneIndex = luceneIndex;
    this.sentenceToWeightedAccVector = new ConcurrentHashMap<>();
    this.wordVectorCache = new ConcurrentHashMap<>();
  }

  public void clearWordVectorCache() {
    this.sentenceToWeightedAccVector.clear();
  }

  public void clearSentenceVectorCache() {
    this.sentenceToWeightedAccVector.clear();
  }

  public @NotNull INDArray getAccVector(@NotNull CoreMap sentence) {
    return sentenceToWeightedAccVector.computeIfAbsent(
      AnnotationUtils.lemmasText(sentence), (key) -> computeAccVector(sentence));
  }

  public @Nullable INDArray getWordVector(@NotNull String word) {
    INDArray cached = wordVectorCache.get(word);
    if (cached != null) {
      return cached;
    }

    INDArray queried = queryWordVector(word);
    if (queried != null) {
      wordVectorCache.put(word, queried);
      return queried;
    }

    return null;
  }

  private @Nullable INDArray queryWordVector(@NotNull String word) {
    try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().scopeOutOfWorkspaces()) {
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

    }
    return null;
  }

  public @NotNull INDArray getAccVector(@NotNull List<CoreMap> sentences, int from, int to) {
    var sideAccVector = Nd4j.zeros(WORD_VECTOR_SIZE);
    var subSentences = sentences.subList(from, to + 1);
    int sentencesSize = subSentences.size();
    for (int i = 0; i < sentencesSize; i++) {
      var sentAccVector = getAccVector(subSentences.get(i));
      sideAccVector.addi(sentAccVector);
    }

    return sideAccVector;
  }

  private @NotNull INDArray computeAccVector(@NotNull CoreMap sentence) {
    INDArray accVector = Nd4j.zeros(Embeddings.WORD_VECTOR_SIZE);
    for (var wordToken : AnnotationUtils.tokens(sentence)) {
      INDArray other = getWordVector(wordToken.tag() + wordToken.lemma());
      if (other != null) {
        log.trace("Word vector is of shape {}", other.shapeInfoToString());
        INDArray otherMulIdf = other.mul(posWeights.getOrDefault(wordToken.tag(), 0.0));
        double idf = 1;
        try {
          idf = luceneIndex.inverseDocumentFrequency(wordToken.lemma());
        } catch (LuceneException ignoreEmptyIndex) {
        }

        log.trace("Word {}, has idf {}", wordToken.lemma(), idf);
        log.trace("Using idf {}", idf);
        otherMulIdf.muli(idf);
        accVector.addi(otherMulIdf);
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

  public Pair<INDArray, INDArray> getWordVectorsWithWeights(@NotNull CoreMap sentence) {
    var tokens = AnnotationUtils.tokens(sentence);
    INDArray matrix = Nd4j.create(tokens.size(), WORD_VECTOR_SIZE);
    INDArray weights = Nd4j.create(tokens.size(), 1);

    int validCount = 0;
    for (var token : tokens) {
      INDArray vector = getWordVector(token.tag() + token.lemma());

      if (vector != null) {
        matrix.putRow(validCount, vector);

        double posWeight = posWeights.getOrDefault(token.tag(), 0.0);
        double idf = 1.0;
        try {
          idf = luceneIndex.inverseDocumentFrequency(token.lemma());
        } catch (LuceneException ignoreEmptyIndex) {
        }

        weights.putScalar(validCount, 0, posWeight * idf);
        validCount++;
      }
    }

    if (validCount == 0) {
      return new Pair<>(Nd4j.empty(), Nd4j.empty());
    }

    INDArray validMatrix = matrix.getRows(IntStream.range(0, validCount).toArray());
    INDArray validWeights = weights.getRows(IntStream.range(0, validCount).toArray());
    return new Pair<>(validMatrix, validWeights);
  }
}
