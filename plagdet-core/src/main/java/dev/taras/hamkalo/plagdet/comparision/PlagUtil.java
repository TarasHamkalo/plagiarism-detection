package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.documents.DocumentPair;

import java.util.Comparator;
import java.util.List;

public abstract class PlagUtil {

  public static double plagScore(DocumentPair documentPair) {
    int totalCoverage =
      coverageFirst(documentPair.getPlagCases()) + coverageSecond(documentPair.getPlagCases());
    int total =
      documentPair.getFirst().getFileData().getContent().length() + documentPair.getSecond().getFileData().getContent().length();

    return (double)  totalCoverage / total;
  }

  public static int coverageFirst(List<PlagCase> plagCases) {
    plagCases.sort(
      Comparator.comparingInt(PlagCase::getFirstStart).thenComparingInt(PlagCase::getFirstEnd)
    );
    int coverage = 0;
    int i = 0;
    while (i < plagCases.size()) {
      int plagStart = plagCases.get(i).getFirstStart();
      int plagEnd = plagCases.get(i).getFirstEnd();

      int j = i + 1;
      while (j < plagCases.size()) {
        if (plagCases.get(j).getFirstStart() < plagEnd) {
          plagEnd = Math.max(plagEnd, plagCases.get(j).getFirstEnd());
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

  public static int coverageSecond(List<PlagCase> plagCases) {
    plagCases.sort(
      Comparator.comparingInt(PlagCase::getSecondStart).thenComparingInt(PlagCase::getSecondEnd)
    );
    int coverage = 0;
    int i = 0;
    while (i < plagCases.size()) {
      int plagStart = plagCases.get(i).getSecondStart();
      int plagEnd = plagCases.get(i).getSecondEnd();

      int j = i + 1;
      while (j < plagCases.size()) {
        if (plagCases.get(j).getSecondStart() < plagEnd) {
          plagEnd = Math.max(plagEnd, plagCases.get(j).getSecondEnd());
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
