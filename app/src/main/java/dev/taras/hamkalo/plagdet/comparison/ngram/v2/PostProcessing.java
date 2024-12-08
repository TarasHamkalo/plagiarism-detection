package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.SharedShingle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostProcessing {
  // Alzahrani 2015 used 300 characters
  private static final int MERGE_DISTANCE = 300;

  public static List<Fragment> expandAndFilter(List<SharedShingle> sharedShingles) {
    long id = 0;
    var result = new ArrayList<Fragment>();
    sharedShingles.sort(Comparator.comparingInt(SharedShingle::getFirstStart));
    for (int i = 0; i < sharedShingles.size(); ) {
      var fragment = new Fragment(id, sharedShingles.get(i));
      int j = i + 1;
      for (; j < sharedShingles.size(); j++) {
        var other = sharedShingles.get(j);

        // goes in increasing order, so valid
        boolean overlapsFirst = other.getFirstStart() <= fragment.getFirstEnd() + MERGE_DISTANCE;
        // goes in random order!
//        boolean overlapsSecond = other.getSecondStart() <= fragment.getSecondEnd() + MERGE_DISTANCE;
        if (!overlapsFirst) {
          // no other shingle could get into this fragment (sorted!)
          break;
        }

        boolean overlapsSecond = other.getSecondStart() >= fragment.getSecondEnd();
        overlapsSecond &= other.getSecondStart() <= fragment.getSecondEnd() + MERGE_DISTANCE;
        if (overlapsSecond) {
          fragment.addShingle(other);
        }
      }

      i = j;
      id++;

      result.add(fragment);
    }

//    return result;
    return filterOverlapping(result);
  }

  /**
   * Expects that overlap could occur only on the second document side
   * as the expansion phase just ensure first document shingles won't overlap
   * @param fragments
   * @return
   */
  private static List<Fragment> filterOverlapping(List<Fragment> fragments) {
    fragments.sort(Comparator.comparingInt(Fragment::getFirstStart));
    List<Fragment> result = new ArrayList<>();

    for (var fragment : fragments) {
      for (Fragment presentFragment : result) {
        if (presentFragment.getSecondEnd() >= fragment.getSecondStart()) {
          fragment.setSecondStart(presentFragment.getSecondEnd());
        }
      }

      if (fragment.secondCovered() > 0) {
        result.add(fragment);
      }
    }

    return result;
  }

//  private static List<Fragment> filterOverlapping(List<Fragment> fragments) {
//    fragments.sort(Comparator.comparingInt(Fragment::getFirstStart));
//    List<Fragment> result = new ArrayList<>();
//
//    for (var fragment : fragments) {
//      for (int i = 0; i < result.size(); i++) {
//        var presentFragment = result.get(i);
//        if (fragmentsOverlap(presentFragment, fragment)) {
//          resolveOverlap(presentFragment, fragment);
//        }
//      }
//    }
//
//    return result;
//  }
//

  /**
   * Expects two consecutive fragments by firstDocument Start position
   *
   * @param first
   * @param second
   * @return
   */
  private static List<Fragment> resolveOverlap(Fragment first, Fragment second) {
////    f0:
//        //fStart =1
//        //fEnd = 830
//        //secondStart = 1
//        //secondEnd = 1247
//    //f1:
//        //fStart = 1349
//        //fEnd  = 4930
//        //secondStart = 239
//        //secondEnd = 3463
//    <fstart, fend> <f0 secondEnd f1SecondEnd>
//      partial overlap results in 2 non overlapping fragments
//    f1.fStart - f1.fEnd, f1.fStart
//    f0.secondStart - f0.secondEnd, f0.secondEnd - f1.secondEnd
    var result = new ArrayList<Fragment>();
    second.setSecondStart(first.getSecondEnd());
    result.add(first);
    result.add(second);
    return result;
  }

  private static boolean fragmentsOverlap(Fragment first, Fragment second) {
    boolean overlapFirst = first.getFirstEnd() >= second.getFirstStart();
    boolean overlapSecond = first.getSecondEnd() >= second.getSecondStart();
    return overlapFirst || overlapSecond;
  }

}
