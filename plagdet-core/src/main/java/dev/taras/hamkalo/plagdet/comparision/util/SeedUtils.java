package dev.taras.hamkalo.plagdet.comparision.util;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Value
@Builder
public class SeedUtils {

  public static @NotNull List<int[]> sortBySide(@NotNull Collection<int[]> seeds, int side) {
    return seeds.stream().sorted(Comparator.comparingInt(seed -> seed[side])).toList();
  }

  public static boolean isAdjacent(int @NotNull [] that, int @NotNull [] other, int side, int gap) {
    return that[2 + side] == other[2 + side] && (Math.abs(that[side] - other[side]) - 1) <= gap;
  }

}
