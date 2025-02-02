package dev.taras.hamkalo.plagdet.documents;


import dev.taras.hamkalo.plagdet.comparision.PlagSource;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentPair implements Comparable<DocumentPair> {

  @NotNull DocumentEntry first;

  @NotNull DocumentEntry second;

  @NotNull
  @Builder.Default
  PlagSource plagSource = PlagSource.UNCERTAIN;

  @NotNull
  @Builder.Default
  List<SpecialMarking> plagCases = new ArrayList<>();

  @NotNull
  @Builder.Default
  List<PlagScore> plagScores = new ArrayList<>();

  @Override
  public int compareTo(@NotNull DocumentPair other) {
    return Double.compare(this.getMaxScore(), other.getMaxScore());
  }

  public double getMaxScore() {
    return getMaxScore(this);
  }

  public static double getMaxScore(@NotNull DocumentPair pair) {
    return pair.getPlagScores().stream()
      .map(PlagScore::getScore)
      .max(Double::compare)
      .orElse(0.0);
  }

  public @NotNull String getId() {
    return getId(this);
  }

  public static @NotNull String getId(DocumentPair documentPair) {
    return documentPair.getFirst().getId() + "_" + documentPair.getSecond().getId();
  }

}
