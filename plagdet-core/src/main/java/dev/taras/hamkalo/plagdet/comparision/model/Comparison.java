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

}
