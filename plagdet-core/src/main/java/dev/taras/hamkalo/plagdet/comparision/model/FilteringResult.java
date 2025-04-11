package dev.taras.hamkalo.plagdet.comparision.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FilteringResult {

  boolean shouldFilter;

  PlagScore plagScore;

}
