package dev.taras.hamkalo.plagdet.comparision.blocks.seeding;

import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SeedingStrategy {

  @NotNull
  List<int[]> seed(@NotNull List<List<CoreMap>> documents);

}
