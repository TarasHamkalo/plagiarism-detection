package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
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
