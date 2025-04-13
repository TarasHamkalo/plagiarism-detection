package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.scorer.WeightedWordAlignmentScorer;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndexBasedWordAlignmentSeedingStrategy implements SeedingStrategy {

  /**
   * Query HNSW for top 10 sentences above that threshold
   */
  public static final float VECTOR_SIMILARITY_THRESHOLD = 0.7f;

  /**
   * Drop every sentence match below that similarity
   */
  public static final float ALIGNMENT_SIMILARITY_THRESHOLD = 0.65f;

  /**
   * Drop every sentence match below that size ratio
   */
  public static final float SIZE_RATIO_TRESHOLD = 0.5f;

  /**
   * Represents count of top sentences to query for, when doing document to document match
   */
  private static final int SIMILAR_SENTENCES_TO_CONSIDER = 10;

  /**
   * Represents count of top sentences to query for, when doing document to corpus match.
   * E.g. having single suspicious document and corpus of documents, for which we don't want results
   * but would like to know if that single could a source document inside that corpus
   */
  private static final int SIMILAR_SENTENCES_TO_CONSIDER_MULTI_SEARCH = 250;

  @NotNull Embeddings embeddings;

  public @NotNull List<int[]> seed(@NotNull DocumentPool documentPool) {
    List<int[]> seeds = new ArrayList<>();
    var suspSentenceIndex = documentPool.getSuspiciousDocumentSentenceIndex();
    var suspiciousDocumentSentenceMatrices = documentPool.getSuspiciousDocumentSentenceMatrices();

    var sourceDocumentSentences =
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation());
    int sourceDocumentSentencesSize = sourceDocumentSentences.size();

    for (int i = 0; i < sourceDocumentSentencesSize; i++) {
      var matches = suspSentenceIndex.findSimilar(
        sourceDocumentSentences.get(i), SIMILAR_SENTENCES_TO_CONSIDER, VECTOR_SIMILARITY_THRESHOLD
      );
      int matchesSize = matches.size();
      // TODO: profile embeddings get word vectors
      var sourceSentenceMatrix = embeddings.getWordVectorsWithWeights(sourceDocumentSentences.get(i));
      for (int j = 0; j < matchesSize; j++) {
        var match = matches.get(j);
        // TODO: validate not ZERO vector, strange matches
        var similarSentenceMatrix = suspiciousDocumentSentenceMatrices.get(match[1]);
        if (WeightedWordAlignmentScorer.areSimilar(
          similarSentenceMatrix, sourceSentenceMatrix, SIZE_RATIO_TRESHOLD, ALIGNMENT_SIMILARITY_THRESHOLD
        )) {
          seeds.add(new int[]{match[1], i});
        }
      }
    }

    return seeds;
  }

  @Override
  public @NotNull Map<Integer, List<int[]>> seedMultiple(
    @NotNull DocumentPool documentPool,
    @NotNull SentenceIndex corpusSentencesIndex
  ) {
    var documentToSeeds = new HashMap<Integer, List<int[]>>();

    var suspiciousDocument = documentPool.getSuspiciousDocument();
    var suspiciousDocumentSentencesSize =
      AnnotationUtils.sentences(suspiciousDocument.getAnnotation()).size();
    var suspiciousDocumentSentenceMatrices = documentPool.getSuspiciousDocumentSentenceMatrices();
    var suspiciousDocumentSentenceIndex = documentPool.getSuspiciousDocumentSentenceIndex();
    for (int i = 0; i < suspiciousDocumentSentencesSize; i++) {
      var matches = corpusSentencesIndex.findSimilar(
        suspiciousDocumentSentenceIndex.findSentenceVectorByGlobalIndex(i),
        SIMILAR_SENTENCES_TO_CONSIDER_MULTI_SEARCH,
        VECTOR_SIMILARITY_THRESHOLD
      );
      var suspiciousSentenceMatrix = suspiciousDocumentSentenceMatrices.get(i);
      int matchesSize = matches.size();
      for (int j = 0; j < matchesSize; j++) {
        var match = matches.get(j);
        var similarSentence = corpusSentencesIndex.findSentenceByGlobalIndex(match[2]);
        // TODO: profile embeddings get word vectors
        var similarSentenceMatrix = embeddings.getWordVectorsWithWeights(similarSentence);
        if (WeightedWordAlignmentScorer.areSimilar(
          similarSentenceMatrix, suspiciousSentenceMatrix, SIZE_RATIO_TRESHOLD, ALIGNMENT_SIMILARITY_THRESHOLD
        )) {
          documentToSeeds.computeIfAbsent(match[0], (key) -> new ArrayList<>())
            .add(new int[]{i, match[1]});
        }
      }
    }

    return documentToSeeds;
  }

}
