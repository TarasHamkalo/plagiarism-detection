package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.similarity;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingle;

public interface SimilarityScorer {

  double score(Shingle first, Shingle second);

}
