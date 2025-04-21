package dev.taras.hamkalo.plagdet.comparision.filter;

import dev.taras.hamkalo.plagdet.comparision.model.FilteringResult;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LengthComparisonFilter implements ComparisonFilter {

  @Value
  @Builder
  public static class LengthComparisonFilterParameters {

    public static final boolean DEFAULT_BYPASS_FILTERING = false;

    public static final double DEFAULT_SENTENCES_SIZE_RATIO_THRESHOLD = 0.5;

    @Builder.Default
    boolean bypassFiltering = DEFAULT_BYPASS_FILTERING;

    @Builder.Default
    double sentenceSizeRatioThreshold = DEFAULT_SENTENCES_SIZE_RATIO_THRESHOLD;
  }

  @NotNull LengthComparisonFilterParameters parameters;

  @Override
  public @NotNull FilteringResult doFilter(@NotNull DocumentPool documentPool) {
    return doFilter(documentPool.getSuspiciousDocument(), documentPool.getSourceDocument());
  }

  @Override
  public @NotNull FilteringResult doFilter(
    @NotNull DocumentEntry suspiciousDocument,
    @NotNull DocumentEntry sourceDocument
  ) {
    double firstSize = 1.0 * AnnotationUtils.sentences(suspiciousDocument.getAnnotation()).size();
    double secondSize = 1.0 * AnnotationUtils.sentences(sourceDocument.getAnnotation()).size();
    double sizeRatio = Math.min(firstSize / secondSize, secondSize / firstSize);
    return FilteringResult.builder()
      .shouldFilter(!parameters.bypassFiltering && sizeRatio < parameters.sentenceSizeRatioThreshold)
      .plagScore(
        PlagScore.builder()
          .score(0)
          .type(PlagScoreType.NONE)
          .comments(Collections.emptyList())
          .build())
      .build();
  }


}