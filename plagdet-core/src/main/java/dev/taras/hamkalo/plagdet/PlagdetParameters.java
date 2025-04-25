package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy.RecursiveClusteringExtensionStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.taras.hamkalo.plagdet.ComparisonsStore.ComparisonsStoreParameters;
import static dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy.*;
import static dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter.LengthComparisonFilterParameters;
import static dev.taras.hamkalo.plagdet.indexing.lucene.LuceneFileDataSource.LuceneFileDataSourceParameters;
import static dev.taras.hamkalo.plagdet.preprocessing.Pipeline.PipelineParameters;


@Value
@Builder
public class PlagdetParameters {

  @Nullable String luceneIndexDirectoryPath;

  @Nullable String templateDocumentPath;

  @NotNull PipelineParameters pipelineParameters;

  @NotNull MetadataComparatorParameters metadataComparatorParameters;

  @NotNull ShingleOverlapComparisonFilterParameters shingleOverlapComparisonFilterParameters;

  @NotNull LengthComparisonFilterParameters lengthComparisonFilterParameters;

  @NotNull ComparisonsStoreParameters comparisonsStoreParameters;

  @NotNull LuceneFileDataSourceParameters luceneFileDataSourceParameters;

  @NotNull IndexBasedWordAlignmentSeedingStrategyParameters seedingStrategyParameters;

  @NotNull RecursiveClusteringExtensionStrategyParameters extensionStrategyParameters;

  @NotNull LengthBasedFilteringStrategyParameters filteringStrategyParameters;
}
