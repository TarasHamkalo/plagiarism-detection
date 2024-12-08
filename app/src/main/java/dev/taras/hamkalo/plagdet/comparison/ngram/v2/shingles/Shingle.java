package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Holds the least comparison unit
 */
@Data
@Builder
@AllArgsConstructor
public class Shingle {

  String content;

  Integer start;

  Integer end;

}