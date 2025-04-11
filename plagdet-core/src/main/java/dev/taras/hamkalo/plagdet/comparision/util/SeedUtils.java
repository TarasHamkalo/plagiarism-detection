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

  public static boolean isAdjacent(int @NotNull [] first, int @NotNull [] other, int side, int gap) {
    return Math.abs(first[side] - other[side]) <= gap;
  }

}
