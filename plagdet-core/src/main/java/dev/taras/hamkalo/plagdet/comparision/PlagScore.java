package dev.taras.hamkalo.plagdet.comparision;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlagScore {

  public enum PlagScoreType {
    META, JACCARD, SEMANTIC
  }

  double score;

  PlagScoreType type;

  @NotNull
  @Builder.Default
  List<String> comments  = new ArrayList<>();
}
