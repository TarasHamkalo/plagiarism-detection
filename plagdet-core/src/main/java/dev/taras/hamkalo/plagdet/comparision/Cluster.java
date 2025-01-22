package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cluster {

  @NotNull List<Seed> seeds;

  public void sortSeedsBySideIndex(int side) {
    seeds.sort(Comparator.comparingInt(s -> s.getSideIndex(side)));
  }

  /**
   * @implNote Should be sorted before calling
   * @return index of first sentence in document specified by side param
   */
  public @Nullable Integer getStartSideIndex(int side) {
    if (firstSeed() != null) {
      return firstSeed().getSideIndex(side);
    }

    return null;
  }

  /**
   * @implNote Should be sorted before calling
   * @return index of last sentence in document specified by side param
   */
  public @Nullable Integer getEndSideIndex(int side) {
    if (lastSeed() != null) {
      return lastSeed().getSideIndex(side);
    }

    return null;
  }

  public @Nullable Seed firstSeed() {
    if (seeds.isEmpty()) {
      return null;
    }

    return seeds.get(0);
  }

  public @Nullable Seed lastSeed() {
    if (seeds.isEmpty()) {
      return null;
    }

    return seeds.get(seeds.size() - 1);
  }

  /**
   * Intersection on side of first document
   * @implNote Other.start should be greater than this.start
   * @param other Cluster for same document pair
   * @return Insersect of all sentences covered by interval this.firstSeed.start - this.lastSeed.end
   *   and other...
   */
  public @Nullable List<CoreSentence> intersect(Cluster other) {
    var coreDocument = firstSeed().getFirstSent().document();
    // should be the same --> var secondCoreDocument = other.firstSeed().getFirstSent().document();

    int firstStart = getStartSideIndex(0);
    int firstEnd = getEndSideIndex(0);
    int secondStart = other.getStartSideIndex(0);
    int secondEnd = other.getEndSideIndex(0);
    int commonStart = Math.max(firstStart, secondStart);
    int commonEnd = Math.min(firstEnd, secondEnd);

    return coreDocument.sentences().subList(commonStart, commonEnd + 1);
  }

  /**
   * Should return the complement of intersection of this and other, so non overlapping parts
   * @implNote Other.start should be greater than this.start
   * @param other Cluster for same document pair
   * @return Complement of overlapping parts of this and other
   */
  public @Nullable List<CoreSentence> complement(Cluster other) {
    var coreDocument = firstSeed().getFirstSent().document();

    int firstStart = getStartSideIndex(0);
    int firstEnd = getEndSideIndex(0);
    int secondStart = other.getStartSideIndex(0);
    int secondEnd = other.getEndSideIndex(0);
    int commonStart = Math.max(firstStart, secondStart);
    int commonEnd = Math.min(firstEnd, secondEnd);

    var complement = coreDocument.sentences().subList(firstStart, commonStart + 1);
    if (commonEnd < firstEnd) {
      complement.addAll(coreDocument.sentences().subList(commonEnd + 1, firstEnd + 1));
    }

    return complement;
  }

  public Cluster copy() {
    return Cluster.builder().seeds(new ArrayList<>(seeds)).build();
  }
}
