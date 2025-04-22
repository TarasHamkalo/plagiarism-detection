package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.SeedingStrategy;
import dev.taras.hamkalo.plagdet.comparision.filter.ComparisonFilter;
import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.model.PlagScoreType;
import dev.taras.hamkalo.plagdet.comparision.util.ScoreUtils;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.corpus.IndexableCorpus;
import dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comparator {

  @NotNull ExtensionStrategy extensionStrategy;

  @NotNull SeedingStrategy seedingStrategy;

  @NotNull MetadataComparator metadataComparator;

  @NotNull List<ComparisonFilter> filters;

  /**
   * Compares suspicious and source documents from document pool.
   * It is required for them to already set and all resources
   * prepared.
   *
   * @return Comparison containing results document pair (susp, source)
   */
  public @NotNull Comparison compare(@NotNull DocumentPool documentPool) {
    DocumentEntry suspiciousDocument = documentPool.getSuspiciousDocument();
    DocumentEntry sourceDocument = documentPool.getSourceDocument();
    List<List<CoreMap>> documents = List.of(
      AnnotationUtils.sentences(suspiciousDocument.getAnnotation()),
      AnnotationUtils.sentences(sourceDocument.getAnnotation())
    );

    List<PlagScore> plagScores = new ArrayList<>();
    if (applyFilter(suspiciousDocument, sourceDocument, plagScores)) {
      return Comparison.builder()
        .firstId(suspiciousDocument.getId())
        .secondId(sourceDocument.getId())
        .plagCases(Collections.emptyList())
        .plagScores(plagScores)
        .plagSource(metadataComparator.inferPlagiarismSource(documentPool))
        .build();
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
      .firstId(suspiciousDocument.getId())
      .secondId(sourceDocument.getId())
      .plagCases(plagCases)
      .plagScores(plagScores)
      .plagSource(metadataComparator.inferPlagiarismSource(documentPool))
      .build();
  }

  private boolean applyFilter(
    @NotNull DocumentEntry suspiciousDocument,
    @NotNull DocumentEntry sourceDocument,
    @NotNull List<PlagScore> plagScores
  ) {
    for (var filter : filters) {
      var filteringResult = filter.doFilter(suspiciousDocument, sourceDocument);
      if (!PlagScoreType.NONE.equals(filteringResult.getPlagScore().getType())) {
        plagScores.add(filteringResult.getPlagScore());
      }

      if (filteringResult.isShouldFilter()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compares suspicious document from document pool to all documents in corpus.
   *
   * @param corpus External corpus to which comparisons should be made
   * @return comparison between suspicious document and some corpus' document
   */
  public @NotNull List<Comparison> compare(
    @NotNull DocumentPool documentPool,
    @NotNull IndexableCorpus corpus
  ) {
    if (!corpus.hasAny()) {
      return Collections.emptyList();
    }

    var suspiciousDocument = documentPool.getSuspiciousDocument();

    var results = new ArrayList<Comparison>();
    var documentSeedsMap = seedingStrategy.seedMultiple(documentPool, corpus.getCorpusSentenceIndex());
    var documentSeedsEntries = documentSeedsMap.entrySet();
    for (var documentSeedsEntry : documentSeedsEntries) {
      int documentIndex = documentSeedsEntry.getKey();
      var sourceDocument = corpus.at(documentIndex);

      List<List<CoreMap>> documents = new ArrayList<>(List.of(
        AnnotationUtils.sentences(suspiciousDocument.getAnnotation()),
        AnnotationUtils.sentences(sourceDocument.getAnnotation())
      ));

      List<PlagScore> plagScores = new ArrayList<>();
      if (applyFilter(suspiciousDocument, sourceDocument, plagScores)) {
        results.add(
          Comparison.builder()
            .firstId(suspiciousDocument.getId())
            .secondId(sourceDocument.getId())
            .plagCases(Collections.emptyList())
            .plagScores(plagScores)
            .plagSource(metadataComparator.inferPlagiarismSource(suspiciousDocument, sourceDocument))
            .build()
        );
        continue;
      }

      List<int[]> seeds = documentSeedsEntry.getValue();
      List<Fragment> fragments = extensionStrategy.extend(documents, seeds);
      List<SpecialMarking> plagCases = fragments.stream()
        .map(f -> f.toMarkingWithType(documents, SpecialMarking.SpecialMarkingType.PLAG))
        .collect(Collectors.toList());

      plagScores.add(
        PlagScore.builder()
          .score(ScoreUtils.plagScoreAsSentenceCoverage(documents, fragments))
          .type(PlagScoreType.SEMANTIC)
          .build()
      );

      results.add(
        Comparison.builder()
        .firstId(suspiciousDocument.getId())
        .secondId(sourceDocument.getId())
        .plagCases(plagCases)
        .plagScores(plagScores)
        .plagSource(metadataComparator.inferPlagiarismSource(suspiciousDocument, sourceDocument))
        .build()
      );
    }
    return results;
  }
}
