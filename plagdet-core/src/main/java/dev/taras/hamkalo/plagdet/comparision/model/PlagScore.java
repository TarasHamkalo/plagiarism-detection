package dev.taras.hamkalo.plagdet.comparision.model;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class PlagScore {

  double score;

  PlagScoreType type;

  @NotNull
  @Builder.Default
  List<String> comments = new ArrayList<>();
}
