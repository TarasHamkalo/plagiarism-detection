package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.SeedingStrategy;
import dev.taras.hamkalo.plagdet.comparision.filter.ComparisonFilter;
import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import dev.taras.hamkalo.plagdet.comparision.util.ScoreUtils;
import dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comparator {

  @NotNull ExtensionStrategy extensionStrategy;

  @NotNull SeedingStrategy seedingStrategy;

  @NotNull MetadataComparator metadataComparator;

  @NotNull List<ComparisonFilter> filters;

  public @NotNull Comparison comparePair(@NotNull DocumentPool documentPool) {
    List<PlagScore> plagScores = new ArrayList<>();

    List<List<CoreMap>> documents = List.of(
      AnnotationUtils.sentences(documentPool.getSuspiciousDocument().getAnnotation()),
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation())
    );

    for (var filter : filters) {
      var filteringResult = filter.doFilter(documentPool);
      if (!PlagScoreType.NONE.equals(filteringResult.getPlagScore().getType())) {
        plagScores.add(filteringResult.getPlagScore());
      }

      if (filteringResult.isShouldFilter()) {
        return Comparison.builder()
          .firstId(documentPool.getSuspiciousDocument().getId())
          .secondId(documentPool.getSourceDocument().getId())
          .plagCases(Collections.emptyList())
          .plagScores(plagScores)
          .plagSource(metadataComparator.inferPlagiarismSource(documentPool))
          .build();
      }
    }

    List<int[]> seeds = seedingStrategy.seed(documentPool);
    List<Fragment> fragments = extensionStrategy.extend(documentPool, seeds);
    List<SpecialMarking> plagCases = fragments.stream()
      .map(f -> f.toMarkingWithType(documents, SpecialMarking.SpecialMarkingType.PLAG))
      .collect(Collectors.toList());
    plagScores.add(
      PlagScore.builder()
        .score(ScoreUtils.plagScoreAsSentenceCoverage(documents, fragments))
        .type(PlagScoreType.SEMANTIC)
        .build()
    );

    return Comparison.builder()
      .firstId(documentPool.getSuspiciousDocument().getId())
      .secondId(documentPool.getSourceDocument().getId())
      .plagCases(plagCases)
      .plagScores(plagScores)
      .plagSource(metadataComparator.inferPlagiarismSource(documentPool))
      .build();
  }


}
