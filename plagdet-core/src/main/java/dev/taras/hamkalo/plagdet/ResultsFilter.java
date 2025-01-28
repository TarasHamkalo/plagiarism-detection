package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

/**
 * Filter results for each document by some threshold, leaving only meaningfull
 */
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResultsFilter {

  @Value
  @Builder
  @AllArgsConstructor
  public static class ResultsFilterParameters {

    public static final int DEFAULT_MAX_RESULTS_PER_DOCUMENT = 3;

    public static final double DEFAULT_MIN_SIMILARITY_TO_INCLUDE = 0.4;

    public static final boolean DEFAULT_BYPASS_RESULTS_FITLER = false;

    @Builder.Default
    public int maxResultsPerDocument = DEFAULT_MAX_RESULTS_PER_DOCUMENT;

    @Builder.Default
    public double minSimilarityToInclude = DEFAULT_MIN_SIMILARITY_TO_INCLUDE;

    @Builder.Default
    public boolean bypassResultsFiltering = DEFAULT_BYPASS_RESULTS_FITLER;

  }

  @NotNull ResultsFilterParameters parameters;

  /**
   * @param documentPair to be validated
   * @return true if pair should be left in results
   */
  public boolean filterDocumentPair(@NotNull DocumentPair documentPair) {
    if (parameters.bypassResultsFiltering) {
      return true;
    }

    return documentPair.getMaxScore() >= parameters.getMinSimilarityToInclude();
  }

  /**
   * @param documentEntry removes all comparisons to fit the limit of MAX_RESULTS_PER_DOCUMENT
   */
  public void safeAddDocumentPair(
    @NotNull DocumentEntry documentEntry,
    @NotNull DocumentPair documentPair
  ) {
//    if (documentEntry.isIndexed() || documentEntry.isTemplate()) {
//      return;
//    }
    documentEntry.getDocumentPairs().offer(documentPair);
    final int documentPairsSize = documentEntry.getDocumentPairs().size();

    if (!parameters.bypassResultsFiltering &&
        documentPairsSize > parameters.getMaxResultsPerDocument()) {
        documentEntry.getDocumentPairs().poll();
    }
  }

}
