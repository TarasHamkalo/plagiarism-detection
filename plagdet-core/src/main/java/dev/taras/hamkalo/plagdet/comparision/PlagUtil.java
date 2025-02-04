package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public abstract class PlagUtil {

  public static double plagScoreAsSentenceCoverage(
    @NotNull DocumentPair documentPair,
    @NotNull List<Fragment> plagiarizedFragments
  ) {

    long totalCoverage = sideSentenceCoverage(plagiarizedFragments, 0) +
                        sideSentenceCoverage(plagiarizedFragments, 1);
    List<CoreSentence> firstSentences = documentPair.getFirst().getCoreDocument().sentences();
    List<CoreSentence> secondSentences = documentPair.getSecond().getCoreDocument().sentences();
    long ignoredCount =
      firstSentences.stream().filter(Predicate.not(PlagUtil::isInformativeSentence)).count() +
      secondSentences.stream().filter(Predicate.not(PlagUtil::isInformativeSentence)).count();

    long total = firstSentences.size() + secondSentences.size();

    if (total == ignoredCount) {
      return 0;
    }

    return (double) totalCoverage / (total - ignoredCount);
  }

  public static double plagScoreAsCharacterCoverage(@NotNull DocumentPair documentPair) {
    // Coverage already excludes template sentences, as they won't appear inside plags
    // (only in infrequent cases)
    int totalCoverage = sideCoverage(documentPair.getPlagCases(), 0) +
                        sideCoverage(documentPair.getPlagCases(), 1);
    int total =
      documentPair.getFirst().getFileData().getContent().length() +
      documentPair.getSecond().getFileData().getContent().length();

    int templateIgnored = sumOfNonOverlappingMarkings(
      documentPair.getPlagCases(),
      documentPair.getFirst().getTemplateMarking(),
      0
    );

    templateIgnored += sumOfNonOverlappingMarkings(
      documentPair.getPlagCases(),
      documentPair.getSecond().getTemplateMarking(),
      1
    );

    int codeIgnored = sumOfNonOverlappingMarkings(
      documentPair.getPlagCases(),
      documentPair.getFirst().getCodeMarking(),
      0
    );

    codeIgnored += sumOfNonOverlappingMarkings(
      documentPair.getPlagCases(),
      documentPair.getSecond().getCodeMarking(),
      1
    );

    int totalIgnored = codeIgnored + templateIgnored;
    if (totalIgnored >= total) {
      return 0;
    }

    return (double) totalCoverage / (total - totalIgnored);
  }


  private static int sumOfNonOverlappingMarkings(
    @NotNull List<SpecialMarking> plagCases,
    @NotNull List<SpecialMarking> ignoredCases,
    int side
  ) {
    var sortedPlagMarkings = plagCases.stream()
      .map(c -> c.getSideMarking(side))
      .sorted()
      .toList();
    var sortedIgnoredMarkings = ignoredCases.stream()
      .map(SpecialMarking::getFirst)
      .sorted()
      .toList();
    int sum = 0;
    int i = 0;
    int j = 0;
    while (i < sortedIgnoredMarkings.size()) {
      while (j < sortedPlagMarkings.size()) {
        if (sortedIgnoredMarkings.get(i).overlaps(sortedPlagMarkings.get(j))) {
          i++;
        } else {
          j++;
          break;
        }
      }

      sum += sortedIgnoredMarkings.get(i).getLength();
      i++;
    }

    return sum;
  }

  public static int sideSentenceCoverage(List<Fragment> fragments, int side) {
    fragments.forEach(f -> f.sortSeedsBySideIndex(side));
    fragments.sort(
      Comparator.comparingInt((Fragment f) -> f.getStartSideIndex(side))
        .thenComparingInt((Fragment f) -> f.getEndSideIndex(side))
    );

    int coverage = 0;
    int i = 0;
    while (i < fragments.size()) {
      int fragStart = fragments.get(i).getStartSideIndex(side);
      int fragEnd = fragments.get(i).getEndSideIndex(side);
      int j = i + 1;
      while (j < fragments.size()) {
        if (fragments.get(j).getStartSideIndex(side) < fragEnd) {
          fragEnd = Math.max(fragments.get(j).getEndSideIndex(side), fragEnd);
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

  public static int sideCoverage(List<SpecialMarking> specialMarkings, int side) {
    specialMarkings.sort(
      Comparator.comparingInt((SpecialMarking m) -> m.getSideMarking(side).getStart())
        .thenComparingInt(m -> m.getSideMarking(side).getEnd())
    );

    int coverage = 0;
    int i = 0;
    while (i < specialMarkings.size()) {

      int plagStart = specialMarkings.get(i).getSideMarking(side).getStart();
      int plagEnd = specialMarkings.get(i).getSideMarking(side).getEnd();
      int j = i + 1;
      while (j < specialMarkings.size()) {
        if (specialMarkings.get(j).getSideMarking(side).getStart() < plagEnd) {
          plagEnd = Math.max(plagEnd, specialMarkings.get(j).getSideMarking(side).getEnd());
          j++;
        } else {
          break;
        }
      }
      coverage += plagEnd - plagStart;
      i = j;
    }

    return coverage;
  }

  public static boolean isInformativeSentence(@NotNull CoreSentence sentence) {
    return isInformative(sentence.coreMap());
  }

  public static boolean isInformative(@NotNull CoreMap coreMap) {
    var stopWordAnnotation = coreMap
      .get(PlagdetAnnotations.StopWordAnnotation.class);

    var codeSegmentAnnotation = coreMap
      .get(PlagdetAnnotations.CodeSegmentAnnotation.class);

    var templateAnnotation = coreMap
      .get(PlagdetAnnotations.TemplateAnnotation.class);

    boolean isStopWord = stopWordAnnotation != null && stopWordAnnotation;
    boolean isCodeSegment = codeSegmentAnnotation != null && codeSegmentAnnotation;
    boolean isTemplate = templateAnnotation != null && templateAnnotation;
    return !isCodeSegment && !isTemplate && !isStopWord;
  }

}
