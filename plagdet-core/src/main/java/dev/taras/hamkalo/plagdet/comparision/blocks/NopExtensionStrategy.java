package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NopExtensionStrategy implements ExtensionStrategy{

  public @NotNull List<Fragment> extend(
    @NotNull DocumentPool documentPool,
    @NotNull List<int[]> seeds
  ) {
    if (seeds.isEmpty()) {
      return Collections.emptyList();
    }

    var fragments = new ArrayList<Fragment>();
    for (int[] seed : seeds) {
      fragments.add(new Fragment(List.of(seed)));
    }

    return fragments;
  }

  public @NotNull List<Fragment> extend(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<int[]> seeds
  ) {
    if (seeds.isEmpty()) {
      return Collections.emptyList();
    }

    var fragments = new ArrayList<Fragment>();
    for (int[] seed : seeds) {
      fragments.add(new Fragment(List.of(seed)));
    }

    return fragments;
  }
}
