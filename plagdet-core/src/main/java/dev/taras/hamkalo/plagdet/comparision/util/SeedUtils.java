package dev.taras.hamkalo.plagdet.comparision.util;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Value
@Builder
public class SeedUtils {

  public static List<int[]> sortBySide(Collection<int[]> seeds, int side) {
    return seeds.stream().sorted(Comparator.comparingInt(seed -> seed[side])).toList();
  }

  public static boolean isAdjacent(int[] first, int[] other, int side, int gap) {
    return Math.abs(first[side] - other[side]) <= gap;
  }

}
