package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.writable.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

@Slf4j
public abstract class PlagUtil {

  public static double plagScore(DocumentPair documentPair) {
    // Coverage already excludes template sentences, as they won't appear inside of plags
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

    if (templateIgnored == total) {
      return 0;
    }

    return (double) totalCoverage / (total - templateIgnored);
  }

  private static int sumOfNonOverlappingMarkings(@NotNull List<SpecialMarking> plagCases,
                                                 @NotNull List<SpecialMarking> ignoredCases,
                                                 int side) {
    var sortedPlagMarkings = plagCases.stream().map(c -> c.getSideMarking(side)).sorted().toList();
    var sortedIgnoredMarkings = ignoredCases.stream().map(SpecialMarking::getFirst).sorted().toList();
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
      i = j + 1;

      coverage += plagEnd - plagStart;
    }

    return coverage;
  }

}
