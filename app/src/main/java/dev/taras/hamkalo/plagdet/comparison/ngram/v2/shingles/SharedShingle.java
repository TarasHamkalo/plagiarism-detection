package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SharedShingle {

  String content;

  Integer firstStart;

  Integer secondStart;

  Integer firstEnd;

  Integer secondEnd;

  public static SharedShingle from(Shingle first, Shingle second) {
    return SharedShingle.builder()
      .firstStart(first.getStart())
      .firstEnd(first.getEnd())
      .secondStart(second.getStart())
      .secondEnd(second.getEnd())
      .content(first.getContent())
      .build();
  }
}

//shingles r two documents should be sorted -> sort by content -> if a and b are sim > threshold -> pair
//Document -> shingles -> {***} -> List<SharedShingle> -> sort(shared) -> {if start1 - start2 < 0 -> merge} -> fragment
//
//V (document 2)
//Document -> shingles -> index -> map merge
