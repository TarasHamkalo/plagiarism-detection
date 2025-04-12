package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComparisonsStore {

  public static final Map<PlagScoreType, Double> PLAG_SCORE_TYPE_WEIGHTS = Map.ofEntries(
    Map.entry(PlagScoreType.NONE, 0.0),
    Map.entry(PlagScoreType.JACCARD, 0.3),
    Map.entry(PlagScoreType.META, 0.5),
    Map.entry(PlagScoreType.SEMANTIC, 1.2)
  );

  @Value
  @Builder
  public static class ComparisonsStoreParameters {
    /**
     * Should we store all results ?
     */
    public static final boolean DEFAULT_BYPASS_FILTERING = false;

    /**
     * How many results per document we are interested in ?
     */
    public static final int DEFAULT_MAX_RESULTS_PER_DOCUMENT = 20;

    /**
     * Minimum similarity to record comparison. All type of scores are considered
     * but when multiple added, priority goes to one with higher semantic score,
     * later shingle and metadata.
     */
    public static final double DEFAULT_MIN_SIMILARITY_TO_RECORD = 0.5f;

    @Builder.Default
    boolean canBypassResultFiltering = DEFAULT_BYPASS_FILTERING;

    @Builder.Default
    int maxResultsPerDocument = DEFAULT_MAX_RESULTS_PER_DOCUMENT;

    @Builder.Default
    double minSimilarityToRecord = DEFAULT_MIN_SIMILARITY_TO_RECORD;
  }

  /**
   * Stores sorted comparisons for each document.
   * When particular document was compared to all corpus documents, those comparisons
   * are flushed to disk and removed.
   */
  @NotNull Map<Integer, PriorityQueue<Comparison>> documentToComparisons;

  @NotNull ComparisonsStoreParameters parameters;

  public ComparisonsStore(
    @NotNull ComparisonsStoreParameters parameters
  ) {
    this.parameters = parameters;
    this.documentToComparisons = new HashMap<>();
  }

  public void recordComparison(@NotNull Comparison comparison) {
    if (!parameters.canBypassResultFiltering &&
        !hasAnyScoreExceedingThreshold(comparison)) {
      return;
    }

    safeAddComparison(comparison, comparison.getFirstId());
    safeAddComparison(comparison, comparison.getSecondId());
  }

  public @NotNull List<Comparison> removeComparisonsByDocumentId(int id) {
    var comparisons = this.documentToComparisons.remove(id);
    if (comparisons == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(comparisons);
  }

  private void safeAddComparison(@NotNull Comparison comparison, int documentId) {
    var comparisons = this.documentToComparisons.computeIfAbsent(
      documentId, (key) -> new PriorityQueue<>(this::prioritizeComparisons)
    );

    comparisons.add(comparison);
    if (parameters.canBypassResultFiltering) {
      return;
    }

    final int storedComparisonsSize = comparisons.size();
    if (storedComparisonsSize > parameters.getMaxResultsPerDocument()) {
      comparisons.poll();
    }
  }

  private boolean hasAnyScoreExceedingThreshold(@NotNull Comparison comparison) {
    return comparison.getPlagScores().stream()
      .anyMatch(p -> p.getScore() > parameters.getMinSimilarityToRecord());
  }

  private int prioritizeComparisons(Comparison that, Comparison other) {
    if (that == null) {
      return -1;
    }

    if (other == null) {
      return 1;
    }

    return Double.compare(computeWeightedPlagScore(that), computeWeightedPlagScore(other));
  }

  private static double computeWeightedPlagScore(@NotNull Comparison comparison) {
    return comparison.getPlagScores().stream().map(
        p -> PLAG_SCORE_TYPE_WEIGHTS.getOrDefault(p.getType(), 0.0) * p.getScore())
      .reduce(Double::sum)
      .orElse(0.0);
  }
}
