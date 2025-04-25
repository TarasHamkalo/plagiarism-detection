package dev.taras.hamkalo.plagdet.comparision.filter;

import dev.taras.hamkalo.plagdet.comparision.model.FilteringResult;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import dev.taras.hamkalo.plagdet.comparision.model.Shingle;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.Sets;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// TODO: remove unused vars
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShingleOverlapComparisonFilter implements ComparisonFilter {

  @Value
  @Builder
  public static class ShingleOverlapComparisonFilterParameters {

    public static final int DEFAULT_SIZE = 2;

    public static final int DEFAULT_OVERLAP = 1;

    public static final double DEFAULT_PLAGIARIZED_LEVEL = 0.8;

    public static final double DEFAULT_DISCARD_LEVEL = 0.1;

    public static final boolean DEFAULT_BYPASS_FILTERING = false;

    @Builder.Default
    int size = DEFAULT_SIZE;

    @Builder.Default
    int overlap = DEFAULT_OVERLAP;

    @Builder.Default
    double plagiarizedLevel = DEFAULT_PLAGIARIZED_LEVEL;

    @Builder.Default
    double discardLevel = DEFAULT_DISCARD_LEVEL;

    @Builder.Default
    boolean canBypassFiltering = DEFAULT_BYPASS_FILTERING;

  }

  @NotNull ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters parameters;

  @NotNull Map<Integer, Set<Shingle>> documentToShingle;

  public ShingleOverlapComparisonFilter(
    @NotNull ShingleOverlapComparisonFilterParameters parameters
  ) {
    if (parameters.overlap > parameters.size - 1) {
      throw new IllegalArgumentException("Overlap should be less than size");
    }

    this.parameters = parameters;
    this.documentToShingle = new ConcurrentHashMap<>();
  }

  @Override
  public @NotNull FilteringResult doFilter(
    @NotNull DocumentEntry suspiciousDocument,
    @NotNull DocumentEntry sourceDocument
  ) {
    var firstShingles = getDocumentShingles(suspiciousDocument);
    var secondShingles = getDocumentShingles(sourceDocument);
    double score = jaccardSimilarity(firstShingles, secondShingles);
    return FilteringResult.builder()
      .shouldFilter(score > parameters.plagiarizedLevel || score < parameters.discardLevel)
      .plagScore(
        PlagScore.builder()
          .score(score)
          .type(PlagScoreType.JACCARD)
          .comments(Collections.emptyList())
          .build())
      .build();
  }

  @Override
  public @NotNull FilteringResult doFilter(@NotNull DocumentPool documentPool) {
    return doFilter(documentPool.getSuspiciousDocument(), documentPool.getSourceDocument());
  }

  private @NotNull Set<Shingle> getDocumentShingles(@NotNull DocumentEntry documentEntry) {
    return generateShingles(documentEntry.getAnnotation());
//    return documentToShingle.computeIfAbsent(
//      documentEntry.getId(), (key) -> generateShingles(documentEntry.getAnnotation())
//    );
  }

  private @NotNull Set<Shingle> generateShingles(@NotNull Annotation annotation) {
    final int step = parameters.size - parameters.overlap;

    var wordTokens = AnnotationUtils.tokens(annotation);
    var shingleSet = new HashSet<Shingle>();
    for (int i = 0; i <= wordTokens.size() - parameters.size; i += step) {
      shingleSet.add(
        Shingle.from(wordTokens.subList(i, i + parameters.size), Shingle::tokensToLemmaString)
      );
    }

    return shingleSet;
  }

  public static double jaccardSimilarity(
    @NotNull Set<Shingle> first,
    @NotNull Set<Shingle> second
  ) {
    double unionSize = Sets.union(first, second).size() * 1.0;
    double intersectionSize = Sets.intersection(first, second).size() * 1.0;

    return intersectionSize / unionSize;
  }
}
