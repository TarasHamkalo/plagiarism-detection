package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NopExtensionStrategy extends ExtensionStrategy {

  public NopExtensionStrategy(Embeddings embeddings, double minFragmentSideSim, int minClusterSize) {
    super(embeddings, minFragmentSideSim, minClusterSize);
  }

  public @NotNull List<Fragment> extend(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<int[]> seeds,
    int sentMaxGap
  ) {
    if (seeds.isEmpty()) {
      return Collections.emptyList();
    }
    Fragment root = new Fragment(seeds);
    var fragments = new ArrayList<Fragment>();
    for (int[] seed : seeds) {
      fragments.add(new Fragment(List.of(seed)));
    }

    return fragments;
  }

}
