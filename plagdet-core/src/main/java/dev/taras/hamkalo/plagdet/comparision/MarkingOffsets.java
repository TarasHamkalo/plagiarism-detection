package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarkingOffsets {

  int start;

  int end;

  int length;

}
