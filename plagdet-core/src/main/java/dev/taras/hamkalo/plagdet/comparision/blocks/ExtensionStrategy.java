package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExtensionStrategy {

  @NotNull List<Fragment> extend(
    @NotNull DocumentPool documentPool,
    @NotNull List<int[]> seeds
  );

}
