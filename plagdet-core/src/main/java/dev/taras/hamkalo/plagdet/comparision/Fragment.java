package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fragment {

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
  public @Nullable List<CoreSentence> intersect(Fragment other) {
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
  public @Nullable List<CoreSentence> complement(Fragment other) {
    var coreDocument = firstSeed().getFirstSent().document();

    int firstStart = getStartSideIndex(0);
    int firstEnd = getEndSideIndex(0);
    int secondStart = other.getStartSideIndex(0);
    int secondEnd = other.getEndSideIndex(0);
    int commonStart = Math.max(firstStart, secondStart);
    int commonEnd = Math.min(firstEnd, secondEnd);


    var complement = new ArrayList<CoreSentence>();
    if (commonStart > firstStart) {
      complement.addAll(coreDocument.sentences().subList(firstStart, commonStart));
    }
    if (commonEnd < firstEnd) {
      complement.addAll(coreDocument.sentences().subList(commonEnd + 1, firstEnd + 1));
    }

    return complement;
  }

  public Fragment copy() {
    return Fragment.builder().seeds(new ArrayList<>(seeds)).build();
  }

  public SpecialMarking toMarkingWithType(SpecialMarkingType type) {
    sortSeedsBySideIndex(0);
    int firstStart = firstSeed().firstOffset();
    int firstEnd = lastSeed().getFirstSent().coreMap()
      .get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

    sortSeedsBySideIndex(1);
    int secondStart = firstSeed().secondOffset();
    int secondEnd = lastSeed().getSecondSent().coreMap()
      .get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

    var firstOffsets = MarkingOffsets.builder()
      .start(firstStart)
      .end(firstEnd)
      .length(firstEnd - firstStart)
      .build();

    var secondOffsets = MarkingOffsets.builder()
      .start(secondStart)
      .end(secondEnd)
      .length(secondEnd - secondStart)
      .build();

    return SpecialMarking.builder()
      .type(type)
      .first(firstOffsets)
      .second(secondOffsets)
      .build();
//    return PlagCase.builder()
//      .firstStart(firstStart)
//      .firstEnd(firstEnd)
//      .secondStart(secondStart)
//      .secondEnd(secondEnd)
//      .firstLen(firstEnd - firstStart)
//      .secondLen(secondEnd - secondStart)
//      .build();
  }
}
