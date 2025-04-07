package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator.SentenceComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator.ShingleComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.TemplateComparator;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource.SimilarDocumentSourceParameters;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.taras.hamkalo.plagdet.ResultsFilter.*;

@Value
@Builder
public class PlagdetParameters {

  @Nullable String luceneIndexDirectoryPath;

  @Nullable String templateDocumentPath;

  @NotNull Integer numberOfProcessors;

  @NotNull SentenceComparatorParameters sentenceComparatorParameters;

  @NotNull ShingleComparatorParameters shingleComparatorParameters;

  @NotNull MetadataComparatorParameters metadataComparatorParameters;

  @NotNull SimilarDocumentSourceParameters similarDocumentSourceParameters;

  @NotNull ResultsFilterParameters resultsFilterParameters;

  @NotNull
  @Builder.Default
  SentenceComparatorParameters templateComparatorParameters =
    SentenceComparatorParameters.builder()
      .minClusterSize(4)
      .diceSimilarityThreshold(0.55)
//      .sentenceVectorSimilarityThreshold(0.8)
      .build();
}
