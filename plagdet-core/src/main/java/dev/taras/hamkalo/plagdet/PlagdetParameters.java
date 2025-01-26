package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator.SentenceComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator.ShingleComparatorParameters;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource.SimilarDocumentSourceParameters;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
@Builder
public class PlagdetParameters {

  @Nullable String luceneIndexDirectoryPath;

  @Nullable String templateDocumentPath;

  @NotNull SentenceComparatorParameters sentenceComparatorParameters;

  @NotNull ShingleComparatorParameters shingleComparatorParameters;

  @NotNull MetadataComparatorParameters metadataComparatorParameters;

  @NotNull SimilarDocumentSourceParameters similarDocumentSourceParameters;

}
