package dev.taras.hamkalo.plagdet.documents;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.PlagSource;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentPair implements Comparable<DocumentPair> {

  @Builder.Default
  @JsonProperty("firstId")
  Integer firstDocumentId = null;

  @Builder.Default
  @JsonProperty("secondId")
  Integer secondDocumentId = null;

  @NotNull
  @JsonIgnore
  DocumentEntry first;

  @NotNull
  @JsonIgnore
  DocumentEntry second;

  @NotNull
  @Builder.Default
  @JsonProperty("source")
  PlagSource plagSource = PlagSource.UNCERTAIN;

  @NotNull
  @Builder.Default
  List<SpecialMarking> plagCases = new ArrayList<>();

  @NotNull
  @Builder.Default
  List<PlagScore> plagScores = new ArrayList<>();

  @JsonProperty("id")
  public @NotNull String getId() {
    return getId(this);
  }

  @JsonGetter("firstId")
  public int getFirstDocumentId() {
    return first.getId();
  }

  @JsonGetter("secondId")
  public int getSecondDocumentId() {
    return second.getId();
  }

  @JsonIgnore
  public double getMaxScore() {
    return getMaxScore(this);
  }

  public static double getMaxScore(@NotNull DocumentPair pair) {
    return pair.getPlagScores().stream()
      .map(PlagScore::getScore)
      .max(Double::compare)
      .orElse(0.0);
  }

  public static @NotNull String getId(DocumentPair documentPair) {
    return documentPair.getFirst().getId() + "_" + documentPair.getSecond().getId();
  }

  @Override
  public int compareTo(@NotNull DocumentPair other) {
    return Double.compare(this.getMaxScore(), other.getMaxScore());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DocumentPair that = (DocumentPair) o;
    return Objects.equals(getFirst(), that.getFirst()) &&
           Objects.equals(getSecond(), that.getSecond());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFirst(), getSecond());
  }
}
