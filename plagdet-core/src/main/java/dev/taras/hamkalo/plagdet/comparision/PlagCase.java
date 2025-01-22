package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlagCase {

  int firstStart;

  int secondStart;

  int firstEnd;

  int secondEnd;

  int firstLen;

  int secondLen;

}
