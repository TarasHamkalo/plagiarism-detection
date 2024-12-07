package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * Holds the least comparison unit
 */
@Value
@Builder
public class Shingle {

  String content;

  Integer start;

  Integer end;

}