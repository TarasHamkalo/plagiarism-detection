package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.SharedShingle;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Holds paired shingles for particular document
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fragment {

  Long id;

//  List<SharedShingle> sharedShingles; // at least one paired shingle

  Integer firstStart;

  Integer firstEnd;

  Integer secondStart;

  Integer secondEnd;

  public Fragment(Long id, SharedShingle sharedShingle) {
    this.id = id;
//    this.sharedShingles = new ArrayList<>(List.of(sharedShingle));

    this.firstStart = sharedShingle.getFirstStart();
    this.firstEnd = sharedShingle.getFirstEnd();

    this.secondStart = sharedShingle.getSecondStart();
    this.secondEnd = sharedShingle.getSecondEnd();
  }

  /**
   * If shingles are overlapping then merge them into single shingle
   * else just add it (! distance between fragment and shingle is not measured here !)
   * @param sharedShingle shingle to be added to collection
   */
  public void addShingle(SharedShingle sharedShingle) {
    this.firstStart = Math.min(firstStart, sharedShingle.getFirstStart());
    this.firstEnd = Math.max(firstEnd, sharedShingle.getFirstEnd());

    this.secondStart = Math.min(secondStart, sharedShingle.getSecondStart());
    this.secondEnd = Math.max(secondEnd, sharedShingle.getSecondEnd());
  }

  public int maxCovered() {
    return Math.max(firstEnd - firstStart, secondEnd - secondStart);
  }

  public int firstCovered() {
    return firstEnd - firstStart;
  }

  public int secondCovered() {
    return secondEnd - secondStart;
  }

}
