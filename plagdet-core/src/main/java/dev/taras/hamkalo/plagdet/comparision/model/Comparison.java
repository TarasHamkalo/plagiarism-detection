package dev.taras.hamkalo.plagdet.comparision.model;

import dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Comparison {

  int firstId;

  int secondId;

  PlagSource plagSource;

  List<PlagScore> plagScores;

  List<SpecialMarking> plagCases;

  public String getId() {
    return Comparison.getId(firstId, secondId);
  }

  public static String getId(int firstId, int secondId) {
    return String.format("%d_%d", firstId, secondId);
  }
}
