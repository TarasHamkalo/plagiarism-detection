package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.similarity;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingle;
import edu.stanford.nlp.util.Sets;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class JaccardScorer implements SimilarityScorer{

  @Override
  public double score(Shingle first, Shingle second) {
    var firstWords = Arrays.stream(first.getContent().split(" ")).collect(Collectors.toSet());
    var secondWords = Arrays.stream(second.getContent().split(" ")).collect(Collectors.toSet());

    return jaccardSimilarity(firstWords, secondWords);
  }

  public static double jaccardSimilarity(Set<String> first, Set<String> second) {
    double unionSize = Sets.union(first, second).size() * 1.0;
    double intersectionSize = Sets.intersection(first, second).size() * 1.0;

    return intersectionSize / unionSize;
  }

}
