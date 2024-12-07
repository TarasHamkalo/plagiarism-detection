package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.SharedShingle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostProcessing {

  private static final int MERGE_DISTANCE = 20;

  public static List<Fragment> expandAndFilter(List<SharedShingle> sharedShingles) {
    long id = 0;
    var result = new ArrayList<Fragment>();
    sharedShingles.sort(Comparator.comparingInt(SharedShingle::getFirstStart));
    for (int i = 0; i < sharedShingles.size();) {
      var fragment = new Fragment(id, sharedShingles.get(i));
      int j = i + 1;
      for (; j < sharedShingles.size(); j++) {
        var other = sharedShingles.get(j);

        boolean overlapsFirst = other.getFirstStart() <= fragment.getFirstEnd() + MERGE_DISTANCE;
        boolean overlapsSecond = other.getSecondStart() <= fragment.getSecondEnd() + MERGE_DISTANCE;
        if (!overlapsFirst) {
          // no other shingle could get into this fragment (sorted!)
          break;
        }

        if (overlapsSecond) {
          fragment.addShingle(other);
        }

//        sharedShingles.remove(j);
//        j--;
      }

      i = j;
      id++;

      result.add(fragment);
    }

    return result;
  }

}
