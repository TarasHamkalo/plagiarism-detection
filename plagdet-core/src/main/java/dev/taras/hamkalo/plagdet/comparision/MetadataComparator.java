package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.comparision.filter.ComparisonFilter;
import dev.taras.hamkalo.plagdet.comparision.model.FilteringResult;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import dev.taras.hamkalo.plagdet.comparision.model.PlagSource;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MetadataComparator implements ComparisonFilter {

  private static final double CREATION_DATE_WEIGHT = 2.0;

  private static final double MODIFICATION_DATE_WEIGHT = 1.5;

  private static final double PRINTED_DATE_WEIGHT = 2.0;

  private static final double TOTAL_EDIT_TIME_WEIGHT = 1.0;

  private static final double REVISIONS_NUMBER_WEIGHT = 1.0;

  @Value
  @Builder
  public static class MetadataComparatorParameters {

    public static final boolean DEFAULT_BYPASS_FILTERING = true;

    @Builder.Default
    boolean canBypassFiltering = DEFAULT_BYPASS_FILTERING;
  }

  @NotNull MetadataComparatorParameters parameters;

  @Override
  public @NotNull FilteringResult doFilter(@NotNull DocumentPool documentPool) {
    var first = documentPool.getSuspiciousDocument();
    var second = documentPool.getSourceDocument();
    return doFilter(first, second);
  }

  public @NotNull FilteringResult doFilter(
    @NotNull DocumentEntry firstDocument,
    @NotNull DocumentEntry secondDocument
  ) {
    var first = firstDocument.getFileData().getMetadata();
    var second = secondDocument.getFileData().getMetadata();
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

    return FilteringResult.builder()
      .shouldFilter(!parameters.canBypassFiltering && score == 1)
      .plagScore(
        PlagScore.builder()
          .score(score)
          .type(PlagScoreType.META)
          .comments(comments)
          .build())
      .build();
  }

  public boolean nullableEquals(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }

    return s1.equals(s2);
  }

  public @NotNull PlagSource inferPlagiarismSource(@NotNull DocumentPool documentPool) {
    var first = documentPool.getSuspiciousDocument();
    var second = documentPool.getSourceDocument();
    return inferPlagiarismSource(first, second);
  }

  public @NotNull PlagSource inferPlagiarismSource(
    @NotNull DocumentEntry first,
    @NotNull DocumentEntry second
  ) {
    if (first.getFileData().isIndexed()) {
      return PlagSource.FIRST;
    }

    if (second.getFileData().isIndexed()) {
      return PlagSource.SECOND;
    }

    double comparisonScore =
      compare(first.getFileData().getMetadata(), second.getFileData().getMetadata());
    if (comparisonScore > 0) {
      return PlagSource.SECOND;
    } else if (comparisonScore < 0) {
      return PlagSource.FIRST;
    }

    return PlagSource.UNCERTAIN;
  }

  /**
   * @return value greater than zero if m2 is source
   */
  public double compare(Metadata m1, Metadata m2) {
    double result = 0;
    long tmp = 0;
    result +=
      CREATION_DATE_WEIGHT * compareNullableDates(m1.getCreationDate(), m2.getCreationDate());
    result +=
      MODIFICATION_DATE_WEIGHT * compareNullableDates(m1.getModificationDate(), m2.getModificationDate());
    result +=
      PRINTED_DATE_WEIGHT * compareNullableDates(m1.getLastPrinted(), m2.getLastPrinted());

    tmp = m2.getTotalEditTime() - m1.getTotalEditTime();
    if (tmp != 0) {
      tmp = tmp / Math.abs(tmp);
      result += tmp * TOTAL_EDIT_TIME_WEIGHT;
    }

    tmp = m2.getRevisionsNumber() - m1.getRevisionsNumber();
    if (tmp != 0) {
      tmp = tmp / Math.abs(tmp);
      result += tmp * REVISIONS_NUMBER_WEIGHT;
    }

    return result;
  }

  private int compareNullableDates(Date d1, Date d2) {
    if (d1 == null && d2 == null) return 0;
    if (d1 == null) return -1;
    if (d2 == null) return 1;
    return d1.compareTo(d2);
  }

}