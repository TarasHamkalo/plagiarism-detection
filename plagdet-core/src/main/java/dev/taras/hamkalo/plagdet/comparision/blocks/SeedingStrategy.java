package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;

public interface SeedingStrategy {

  @NotNull List<int[]> seed(
    @NotNull SentenceIndex suspiciousDocumentSentenceIndex,
    @NotNull List<Pair<INDArray, INDArray>> suspiciousDocumentSentenceMatrices,
    @NotNull List<CoreMap> sourceDocumentSentences
  );

  @NotNull List<int[]> seed(@NotNull DocumentPool documentPool);

  @NotNull Map<Integer, List<int[]>> seedMultiple(
    @NotNull DocumentPool documentPool,
    @NotNull SentenceIndex sentenceIndex
  );

}
