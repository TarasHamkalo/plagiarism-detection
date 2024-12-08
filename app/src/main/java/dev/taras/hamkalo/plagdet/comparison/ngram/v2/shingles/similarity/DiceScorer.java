package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.similarity;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingle;
import edu.stanford.nlp.util.Sets;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Set;

public class DiceScorer implements SimilarityScorer {

  @Override
  public double score(Shingle first, Shingle second) {
    var firstWords = Arrays.stream(first.getContent().split(" ")).collect(Collectors.toSet());
    var secondWords = Arrays.stream(second.getContent().split(" ")).collect(Collectors.toSet());

    return diceCoefficient(firstWords, secondWords);
  }

  public static double diceCoefficient(Set<String> first, Set<String> second) {
    double intersectionSize = Sets.intersection(first, second).size() * 1.0;

    return 2 * intersectionSize / (first.size() + second.size());
  }

}
