package dev.taras.hamkalo.plagdet.comparision.util;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

@Slf4j
public abstract class ScoreUtils {

  public static double plagScoreAsSentenceCoverage(
    @NotNull DocumentPair documentPair,
    @NotNull List<Fragment> plagiarizedFragments
  ) {
    var firstAnnotation = documentPair.getFirst().getAnnotation();
    var secondAnnotation = documentPair.getSecond().getAnnotation();

    double totalCoverage = sideSentenceCoverage(plagiarizedFragments, 0) +
                           sideSentenceCoverage(plagiarizedFragments, 1);
    long firstSentencesCount = AnnotationUtils.sentences(firstAnnotation).size();
    long secondSentencesCount = AnnotationUtils.sentences(secondAnnotation).size();
    long total = firstSentencesCount + secondSentencesCount;
    if (total == 0) {
      return 0;
    }

    return totalCoverage / total;
  }


  public static int sideSentenceCoverage(List<Fragment> fragments, int side) {
    fragments.sort(
      Comparator.comparingInt((Fragment f) -> f.getSideStarts()[side])
        .thenComparingInt((Fragment f) -> f.getSideEnds()[side])
    );

    int coverage = 0;
    int i = 0;
    while (i < fragments.size()) {
      int fragStart = fragments.get(i).getSideStarts()[side];
      int fragEnd = fragments.get(i).getSideEnds()[side];
      int j = i + 1;
      while (j < fragments.size()) {
        if (fragments.get(j).getSideStarts()[side] <= fragEnd) {
          fragEnd = Math.max(fragments.get(j).getSideEnds()[side], fragEnd);
          j++;
        } else {
          break;
        }
      }
      coverage += fragEnd - fragStart + 1;
      i = j;
    }

    return coverage;

  }
}
