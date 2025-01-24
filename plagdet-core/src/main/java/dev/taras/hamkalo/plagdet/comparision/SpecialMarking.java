package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
@With
@Builder
public class SpecialMarking {

  public enum SpecialMarkingType {
    PLAG, CODE, TEMPLATE
  }

  @NotNull SpecialMarkingType type;

  @NotNull MarkingOffsets first;

  @Nullable MarkingOffsets second;

  public MarkingOffsets getSideOffsets(int side) {
    return (side % 2 == 0) ? first : second;
  }
}
