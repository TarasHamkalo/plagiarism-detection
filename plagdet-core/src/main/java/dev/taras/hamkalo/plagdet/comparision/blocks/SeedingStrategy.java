package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SeedingStrategy {

  @NotNull List<int[]> seed(@NotNull DocumentPool documentPool);

}
