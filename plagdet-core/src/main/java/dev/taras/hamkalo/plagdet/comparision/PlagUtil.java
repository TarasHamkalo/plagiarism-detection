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

  public static int coverageFirst(List<SpecialMarking> specialMarkings) {
    specialMarkings.sort(
      Comparator.comparingInt((SpecialMarking m) -> m.getFirst().getStart())
        .thenComparingInt(m -> m.getFirst().getEnd())
    );

//    plagCases.sort(
//      Comparator.comparingInt(PlagCase::getFirstStart).thenComparingInt(PlagCase::getFirstEnd)
//    );
    int coverage = 0;
    int i = 0;
    while (i < specialMarkings.size()) {
//      int plagStart = plagCases.get(i).getFirstStart();
//      int plagEnd = plagCases.get(i).getFirstEnd();
      int plagStart = specialMarkings.get(i).getFirst().getStart();
      int plagEnd = specialMarkings.get(i).getFirst().getEnd();;

      int j = i + 1;
      while (j < specialMarkings.size()) {
        if (specialMarkings.get(j).getFirst().getStart() < plagEnd) {
//        if (plagCases.get(j).getFirstStart() < plagEnd) {
          plagEnd = Math.max(plagEnd, specialMarkings.get(j).getFirst().getEnd());
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

  public static int coverageSecond(List<SpecialMarking> specialMarkings) {
    specialMarkings.sort(
      Comparator.comparingInt((SpecialMarking m) -> m.getSecond().getStart())
        .thenComparingInt(m -> m.getSecond().getEnd())
    );

//    plagCases.sort(
//      Comparator.comparingInt(PlagCase::getSecondStart).thenComparingInt(PlagCase::getSecondEnd)
//    );
    int coverage = 0;
    int i = 0;
    while (i < specialMarkings.size()) {
//      int plagStart = plagCases.get(i).getSecondStart();
//      int plagEnd = plagCases.get(i).getSecondEnd();
      int plagStart = specialMarkings.get(i).getSecond().getStart();
      int plagEnd = specialMarkings.get(i).getSecond().getEnd();

      int j = i + 1;
      while (j < specialMarkings.size()) {
        if (specialMarkings.get(j).getSecond().getStart() < plagEnd) {
          plagEnd = Math.max(plagEnd, specialMarkings.get(j).getSecond().getEnd());
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
