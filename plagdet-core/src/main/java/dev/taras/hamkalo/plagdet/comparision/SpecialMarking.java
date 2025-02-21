package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value
@With
@Builder
public class SpecialMarking {

  public enum SpecialMarkingType {
    PLAG, CODE, TEMPLATE, MISSPELLED, SPOOF
  }

  @NotNull SpecialMarkingType type;

  @NotNull MarkingOffsets first;

  @Nullable MarkingOffsets second;

  @Nullable List<String> comments;

  public MarkingOffsets getSideMarking(int side) {
    return (side % 2 == 0) ? first : second;
  }

}
