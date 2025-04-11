package dev.taras.hamkalo.plagdet.comparision.model;

import dev.taras.hamkalo.plagdet.documents.markings.MarkingOffsets;
import dev.taras.hamkalo.plagdet.comparision.util.SeedUtils;
import dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking.SpecialMarkingType;

@Value
public class Fragment {

  List<List<int[]>> sortedSeeds;

  int[] sideStarts;

  int[] sideEnds;

  public Fragment(@NotNull Collection<int[]> seeds) {
    this.sortedSeeds = List.of(SeedUtils.sortBySide(seeds, 0), SeedUtils.sortBySide(seeds, 1));
    this.sideStarts = new int[]{first(0)[0], first(1)[1]};
    this.sideEnds = new int[]{last(0)[0], last(1)[1]};
  }

  public Fragment(@NotNull Fragment source, int from, int to, int side) {
    var seeds = source.getSeeds(side).subList(from, to + 1);
    if (side % 2 == 0) {
      this.sortedSeeds = List.of(seeds, SeedUtils.sortBySide(seeds, 1));
    } else {
      this.sortedSeeds = List.of(SeedUtils.sortBySide(seeds, 0), seeds);
    }

    this.sideStarts = new int[]{first(0)[0], first(1)[1]};
    this.sideEnds = new int[]{last(0)[0], last(1)[1]};
  }

  private int[] first(int side) {
    return sortedSeeds.get(side).get(0);
  }

  private int[] last(int side) {
    return sortedSeeds.get(side).get(sortedSeeds.get(side).size() - 1);
  }

  public @NotNull List<int[]> getSeeds(int side) {
    return sortedSeeds.get(side);
  }

//  public boolean overlaps(Fragment other, int side) {
//    return (other.sideStarts[side] >= this.sideStarts[side] &&
//            other.sideStarts[side] <= this.sideEnds[side]) ||
//           (this.sideStarts[side] >= other.sideStarts[side] &&
//            this.sideStarts[side] <= other.sideEnds[side]);
//  }

  public @NotNull SpecialMarking toMarkingWithType(
    @NotNull List<List<CoreMap>> documents,
    @NotNull SpecialMarkingType specialMarkingType
  ) {
    int firstStart = AnnotationUtils.begin(documents.get(0).get(first(0)[0]));
    int firstEnd = AnnotationUtils.end(documents.get(0).get(last(0)[0]));
    var firstOffsets = new MarkingOffsets(firstStart, firstEnd);

    int secondStart = AnnotationUtils.begin(documents.get(1).get(first(1)[1]));
    int secondEnd = AnnotationUtils.end(documents.get(1).get(last(1)[1]));
    var secondOffsets = new MarkingOffsets(secondStart, secondEnd);
    return SpecialMarking.builder()
      .type(specialMarkingType)
      .first(firstOffsets)
      .second(secondOffsets)
      .build();
  }
}
