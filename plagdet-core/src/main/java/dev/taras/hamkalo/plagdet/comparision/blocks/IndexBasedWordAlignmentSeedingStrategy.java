package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.scorer.WeightedWordAlignmentScorer;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

  @NotNull Embeddings embeddings;

  public @NotNull List<int[]> seed(@NotNull DocumentPool documentPool) {
    List<int[]> seeds = new ArrayList<>();
    var suspSentenceIndex = documentPool.getSuspiciousDocumentSentenceIndex();
    var suspiciousDocumentSentenceMatrices = documentPool.getSuspiciousDocumentSentenceMatrices();

    var sourceDocumentSentences =
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation());
    int sourceDocumentSentencesSize = sourceDocumentSentences.size();

    for (int i = 0; i < sourceDocumentSentencesSize; i++) {
      var matches = suspSentenceIndex.findSimilar(sourceDocumentSentences.get(i), 10, VECTOR_SIMILARITY_THRESHOLD);
      int matchesSize = matches.size();
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
}
