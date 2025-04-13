package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface SeedingStrategy {

  @NotNull List<int[]> seed(@NotNull DocumentPool documentPool);

  @NotNull Map<Integer, List<int[]>> seedMultiple(
    @NotNull DocumentPool documentPool,
    @NotNull SentenceIndex sentenceIndex
  );

}
