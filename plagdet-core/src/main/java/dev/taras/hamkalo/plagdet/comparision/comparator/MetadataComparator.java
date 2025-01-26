package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.PlagDirection;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.io.Metadata;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MetadataComparator implements DocumentComparator {

  private static final double CREATION_DATE_WEIGHT = 2.0;

  private static final double MODIFICATION_DATE_WEIGHT = 1.5;

  private static final double PRINTED_DATE_WEIGHT = 2.0;

  private static final double TOTAL_EDIT_TIME_WEIGHT = 1.0;

  private static final double REVISIONS_NUMBER_WEIGHT = 1.0;

  @Value
  @Builder
  @AllArgsConstructor
  public static class MetadataComparatorParameters {

    public static final boolean DEFAULT_BYPASS_FILTERING = false;

    @Builder.Default
    boolean bypassFiltering = DEFAULT_BYPASS_FILTERING;

  }

  MetadataComparatorParameters parameters;

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    var first = documentPair.getFirst().getFileData().getMetadata();
    var second = documentPair.getSecond().getFileData().getMetadata();

    var score = 0;
    var comments = new ArrayList<String>();
    if (nullableEquals(first.getCreator(), second.getCreator())) {
      comments.add("Same creator: %s".formatted(first.getCreator()));
      score = 1;
    }

    if (nullableEquals(first.getModifier(), second.getModifier())) {
      comments.add("Same modifier: %s".formatted(first.getModifier()));
      score = 1;
    }

    PlagDirection direction;
    if (!documentPair.getFirst().isIndexed() && !documentPair.getSecond().isIndexed()) {
      double plagDirectionScore = compareAndComment(first, second, comments);
      direction = inferPlagiarismDirection(plagDirectionScore);
    } else if (documentPair.getFirst().isIndexed()) {
      direction = PlagDirection.SECOND;
    } else {
      direction = PlagDirection.FIRST;
    }

    documentPair.getPlagScores().add(
      PlagScore.builder()
        .score(score)
        .type(PlagScore.PlagScoreType.META)
        .comments(comments)
        .build()
    );

    documentPair.setDirection(direction);
    if (parameters.bypassFiltering) {
      return ComparisonResult.CONTINUE;
    }

    return score == 1 ? ComparisonResult.PLAGIARIZED : ComparisonResult.CONTINUE;
  }

  public boolean nullableEquals(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }

    return s1.equals(s2);
  }

  public PlagDirection inferPlagiarismDirection(double comparisonScore) {
    if (comparisonScore < 0) {
      return PlagDirection.FIRST;
    } else if (comparisonScore > 0) {
      return PlagDirection.SECOND;
    }

    return PlagDirection.UNCERTAIN;
  }

  /**
   * @return value greater than zero if m1 has higher probability to plagiarize from m2
   */
  public double compareAndComment(Metadata m1, Metadata m2, List<String> comments) {
    double result = 0;

    long tmp = compareNullableDates(m1.getCreationDate(), m2.getCreationDate());
    if (tmp != 0) {
      result += tmp * CREATION_DATE_WEIGHT;
      comments.add((tmp < 0 ? "First" : "Second") + " document created earlier.");
    }

    tmp = compareNullableDates(m1.getModificationDate(), m2.getModificationDate());
    if (tmp != 0) {
      result += tmp * MODIFICATION_DATE_WEIGHT;
      comments.add((tmp > 0 ? "First" : "Second") + " document modified later.");
    }

    tmp = compareNullableDates(m1.getLastPrinted(), m2.getLastPrinted());
    if (tmp != 0) {
      result += tmp * PRINTED_DATE_WEIGHT;
      comments.add((tmp < 0 ? "First" : "Second") + " document was printed earlier.");
    }

    tmp = m1.getTotalEditTime() - m2.getTotalEditTime();
    if (tmp != 0) {
      result += tmp * TOTAL_EDIT_TIME_WEIGHT;
      comments.add((tmp > 0 ? "First" : "Second") + " document was edited more.");
    }

    tmp = m1.getRevisionsNumber() - m2.getRevisionsNumber();
    if (tmp != 0) {
      result += tmp * REVISIONS_NUMBER_WEIGHT;
      comments.add((tmp > 0 ? "First" : "Second") + " document was saved more.");
    }

    return result;
  }

  private int compareNullableDates(Date d1, Date d2) {
    if (d1 == null && d2 == null) return 0;
    if (d1 == null) return 1;
    if (d2 == null) return -1;
    return d1.compareTo(d2);
  }

}
