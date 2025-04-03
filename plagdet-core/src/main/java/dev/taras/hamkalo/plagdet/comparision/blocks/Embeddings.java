package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
  private static volatile Word2Vec word2Vec;

  @NotNull
  LuceneIndex luceneIndex;

  @NotNull
  Map<String, INDArray> sentenceToWeightedAccVector; // TODO: clear cache

  public Embeddings(@NotNull LuceneIndex luceneIndex) {
    this.sentenceToWeightedAccVector = new ConcurrentHashMap<>();
    this.luceneIndex = luceneIndex;
  }

  public @NotNull INDArray getAccVector(@NotNull CoreMap sentence) {
    return sentenceToWeightedAccVector.computeIfAbsent(
      AnnotationUtils.lemmasText(sentence), (key) -> computeAccVector(sentence));
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

  public WordVectors wordVectors() {
    if (word2Vec == null) {
      synchronized (Embeddings.class) {
        if (word2Vec == null) {
          word2Vec = loadWordVectors();
        }
      }
    }
    return word2Vec;
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
    for (var wordToken : AnnotationUtils.wordTokens(sentence)) {
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

  public @NotNull List<INDArray> getWordVectorsList(@NotNull CoreMap first) {
    var tokens = AnnotationUtils.wordTokens(first);
    var wordVectors = new ArrayList<INDArray>();
    for (var token : tokens) {
      INDArray vector = getWordVector(token.tag() + token.lemma());
      if (vector != null) {
        wordVectors.add(vector);
      }
    }

    return wordVectors;
  }

  public @NotNull INDArray getWordVectorsMatrix(@NotNull CoreMap sentence) {
    var tokens = AnnotationUtils.wordTokens(sentence);
    var validVectors = tokens.stream()
      .map(token -> getWordVector(token.tag() + token.lemma()))
      .filter(Objects::nonNull)
      .toList();

    if (validVectors.isEmpty()) {
      return Nd4j.empty();
    }

    // (rows = words, cols = embedding dimensions)
    return Nd4j.vstack(validVectors);
  }

  public Pair<INDArray, INDArray> getWordVectorsWithWeights(@NotNull CoreMap sentence) {
    var tokens = AnnotationUtils.wordTokens(sentence);

    List<INDArray> validVectors = new ArrayList<>();
    List<Double> validWeights = new ArrayList<>();
    for (var token : tokens) {
      INDArray vector = getWordVector(token.tag() + token.lemma());

      if (vector != null) {
        validVectors.add(vector);
        double posWeight = posWeights.getOrDefault(token.tag(), 0.0);
        double idf = 1;
        try {
          idf = luceneIndex.inverseTermFrequency(token.lemma());
        } catch (IndexingException ignoreEmptyIndex) {
        }

        validWeights.add(posWeight * idf);
      }
    }

    if (validVectors.isEmpty()) {
      return new Pair<>(Nd4j.empty(), Nd4j.empty());
    }

    return new Pair<>(Nd4j.vstack(validVectors), Nd4j.create(validWeights.stream().mapToDouble(value -> value).toArray()));
  }
}
