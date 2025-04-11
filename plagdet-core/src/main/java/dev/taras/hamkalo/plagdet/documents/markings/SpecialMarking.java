package dev.taras.hamkalo.plagdet.documents.markings;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@With
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
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
