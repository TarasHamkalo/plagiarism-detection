package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FilteringStrategy {

  @NotNull List<Fragment> filter(
    @NotNull DocumentPool documentPool,
    @NotNull List<Fragment> fragments
  );

  @NotNull List<Fragment> filter(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<Fragment> fragments
  );
}
