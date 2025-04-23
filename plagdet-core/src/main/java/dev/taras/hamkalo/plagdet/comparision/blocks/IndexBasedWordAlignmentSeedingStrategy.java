package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.scorer.WeightedWordAlignmentScorer;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IndexBasedWordAlignmentSeedingStrategy implements SeedingStrategy {


  @Value
  @Builder
  public static class IndexBasedWordAlignmentSeedingStrategyParameters {

    /**
     * Query HNSW for top K sentences above that threshold
     */
    public static final float DEFAULT_VECTOR_SIMILARITY_THRESHOLD = 0.6f;

    /**
     * Drop every sentence match below that similarity by Word alignment score
     */
    public static final float DEFAULT_ALIGNMENT_SIMILARITY_THRESHOLD = 0.75f;

    /**
     * Drop every sentence match below that size ratio
     */
    public static final float DEFAULT_SIZE_RATIO_THRESHOLD = 0.5f;

    /**
     * Represents the least amount of top sentences to query for during Index search
     */
    public static final int DEFAULT_MIN_SENTENCES_TO_CONSIDER = 4;

    /**
     * Percentage of the number of corpus sentences that will be considered during Index search
     */
    public static final double DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER = 0.1d;

    /**
     * Represents the hard limit for count of top sentences to query for, when doing document to corpus match.
     * e.g. having single suspicious document and corpus of documents, for which we don't want results
     * but would like to know if that single could a source document inside that corpus
     */
    public static final double DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER_MULTI_SEARCH = 0.1d;


    /**
     * Represents default per document count of top sentences to query for.
     * e.g. having 10 documents, for each sentence will be queried 10 * 5 top sentences from Index
     */
    public static final int DEFAULT_SENTENCES_PER_DOCUMENT_TO_CONSIDER_MULTI_SEARCH = 5;

    @Builder.Default
    float vectorSimilarityThreshold = DEFAULT_VECTOR_SIMILARITY_THRESHOLD;

    @Builder.Default
    float alignmentSimilarityThreshold = DEFAULT_ALIGNMENT_SIMILARITY_THRESHOLD;

    @Builder.Default
    float sizeRatioThreshold = DEFAULT_SIZE_RATIO_THRESHOLD;

    @Builder.Default
    int minSentencesToConsider = DEFAULT_MIN_SENTENCES_TO_CONSIDER;

    @Builder.Default
    double portionOfSentencesToConsider = DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER;

    @Builder.Default
    double portionOfSentencesToConsiderMultiSearch = DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER_MULTI_SEARCH;

    @Builder.Default
    int sentencesPerDocumentToConsiderMultiSearch = DEFAULT_SENTENCES_PER_DOCUMENT_TO_CONSIDER_MULTI_SEARCH;
  }

  /**
   * Caches sentence word matrices for each document in collection
   * Sentence global list, cached in sentence index is flat sentence collection for fast lookup
   * The representation stored here, is matrix and weights for each sentence,
   * goal is to free it after document was compared, so it will be more
   * efficient to store organized
   */
  @NotNull Map<Integer, Map<Integer, Pair<INDArray, INDArray>>> docToSentenceVectors;

  @NotNull Embeddings embeddings;

  @NotNull IndexBasedWordAlignmentSeedingStrategyParameters parameters;

  public IndexBasedWordAlignmentSeedingStrategy(
    @NotNull Embeddings embeddings,
    @NotNull IndexBasedWordAlignmentSeedingStrategyParameters parameters
  ) {
    this.docToSentenceVectors = new HashMap<>();
    this.embeddings = embeddings;
    this.parameters = parameters;
  }

  public @NotNull List<int[]> seed(@NotNull DocumentPool documentPool) {
    var suspiciousDocumentSentenceIndex = documentPool.getSuspiciousDocumentSentenceIndex();
    var suspiciousDocumentSentenceMatrices = documentPool.getSuspiciousDocumentSentenceMatrices();
    var sourceDocumentSentences =
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation());

    return seed(
      suspiciousDocumentSentenceIndex, suspiciousDocumentSentenceMatrices, sourceDocumentSentences
    );
  }

  public @NotNull List<int[]> seed(
    @NotNull SentenceIndex suspiciousDocumentSentenceIndex,
    @NotNull List<Pair<INDArray, INDArray>> suspiciousDocumentSentenceMatrices,
    @NotNull List<CoreMap> sourceDocumentSentences
  ) {
    List<int[]> seeds = new ArrayList<>();
    int sourceDocumentSentencesSize = sourceDocumentSentences.size();
    int suspiciousSentencesSize = suspiciousDocumentSentenceMatrices.size();
    int similarSentenceToConsider = (int) Math.max(
      parameters.minSentencesToConsider,
      Math.ceil(parameters.portionOfSentencesToConsider * suspiciousSentencesSize)
    );
    for (int i = 0; i < sourceDocumentSentencesSize; i++) {
      var matches = suspiciousDocumentSentenceIndex.findSimilar(
        sourceDocumentSentences.get(i),
        similarSentenceToConsider,
        parameters.vectorSimilarityThreshold
      );
      int matchesSize = matches.size();
      var sourceSentenceMatrix =
        embeddings.getWordVectorsWithWeights(sourceDocumentSentences.get(i));
      for (int j = 0; j < matchesSize; j++) {
        var match = matches.get(j);
        var similarSentenceMatrix = suspiciousDocumentSentenceMatrices.get(match[1]);
        boolean areSimilar = WeightedWordAlignmentScorer.areSimilar(
          similarSentenceMatrix,
          sourceSentenceMatrix,
          parameters.sizeRatioThreshold,
          parameters.alignmentSimilarityThreshold
        );

        if (areSimilar) {
          int id = AnnotationUtils.textBlockId(sourceDocumentSentences.get(i));
          seeds.add(new int[]{match[1], i, match[3], id});
        }
      }
    }

    return seeds;
  }

  @Override
  public @NotNull Map<Integer, List<int[]>> seedMultiple(
    @NotNull DocumentPool documentPool,
    @NotNull SentenceIndex corpusSentencesIndex,
    int indexedDocumentsCount
  ) {
    var documentToSeeds = new HashMap<Integer, List<int[]>>();
    var suspiciousDocument = documentPool.getSuspiciousDocument();
    List<Integer> sentenceTextBlockIds =
      AnnotationUtils.sentences(suspiciousDocument.getAnnotation())
        .stream()
        .map(AnnotationUtils::textBlockId)
        .toList();

    var suspiciousDocumentSentencesSize = sentenceTextBlockIds.size();
    var suspiciousDocumentSentenceMatrices = documentPool.getSuspiciousDocumentSentenceMatrices();
    var suspiciousDocumentSentenceIndex = documentPool.getSuspiciousDocumentSentenceIndex();
    int similarSentenceToConsider = (int) Math.min(
      Math.ceil(parameters.portionOfSentencesToConsiderMultiSearch * corpusSentencesIndex.getIndexSize()),
      parameters.sentencesPerDocumentToConsiderMultiSearch * indexedDocumentsCount
    );

    log.trace(
      "Considering [{}] when there is [{}] sentences\n",
      similarSentenceToConsider,
      corpusSentencesIndex.getGlobalSentencesList().size()
    );
    for (int i = 0; i < suspiciousDocumentSentencesSize; i++) {
      var matches = corpusSentencesIndex.findSimilar(
        suspiciousDocumentSentenceIndex.findSentenceVectorByGlobalIndex(i),
        similarSentenceToConsider,
        parameters.vectorSimilarityThreshold
      );
      var suspiciousSentenceMatrix = suspiciousDocumentSentenceMatrices.get(i);
      int matchesSize = matches.size();
      for (int j = 0; j < matchesSize; j++) {
        var match = matches.get(j);
        var similarSentenceMatrix = getSentenceMatrixForMatch(corpusSentencesIndex, match);
        boolean areSimilar = WeightedWordAlignmentScorer.areSimilar(
          similarSentenceMatrix,
          suspiciousSentenceMatrix,
          parameters.sizeRatioThreshold,
          parameters.alignmentSimilarityThreshold
        );

        if (areSimilar) {
          documentToSeeds.computeIfAbsent(match[0], (key) -> new ArrayList<>())
            .add(new int[]{i, match[1], sentenceTextBlockIds.get(i), match[3]});
        }
      }
    }

    docToSentenceVectors.clear();
    return documentToSeeds;
  }

  private @NotNull Pair<INDArray, INDArray> getSentenceMatrixForMatch(
    @NotNull SentenceIndex sentenceIndex,
    int[] match
  ) {
    var sentence = sentenceIndex.findSentenceByGlobalIndex(match[2]);
    var sentenceCache =
      docToSentenceVectors.computeIfAbsent(match[0], (key) -> new HashMap<>());

    return sentenceCache.computeIfAbsent(
      match[1], (key) -> embeddings.getWordVectorsWithWeights(sentence)
    );
  }
}
