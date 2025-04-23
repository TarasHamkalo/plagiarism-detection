package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.ComparisonsStore.ComparisonsStoreParameters;
import dev.taras.hamkalo.plagdet.comparision.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy.RecursiveClusteringExtensionStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter.LengthComparisonFilterParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneFileDataSource.LuceneFileDataSourceParameters;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline.PipelineParameters;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

public class OptionsMapper {

  public static int getOptionInt(@NotNull CommandLine cmd, String option, int defaultValue) {
    return Integer.parseInt(cmd.getOptionValue(option, String.valueOf(defaultValue)));
  }

  public static double getOptionDouble(@NotNull CommandLine cmd, String option, double defaultValue) {
    return Double.parseDouble(cmd.getOptionValue(option, String.valueOf(defaultValue)));
  }

  public static float getOptionFloat(@NotNull CommandLine cmd, String option, float defaultValue) {
    return Float.parseFloat(cmd.getOptionValue(option, String.valueOf(defaultValue)));
  }

  public static ComparisonsStoreParameters buildComparisonsStoreParameters(@NotNull CommandLine cmd) {
    return ComparisonsStoreParameters.builder()
      .canBypassFiltering(cmd.hasOption("bypass-results-filter"))
      .maxResultsPerDocument(getOptionInt(cmd, "max-results", ComparisonsStoreParameters.DEFAULT_MAX_RESULTS_PER_DOCUMENT))
      .minSimilarityToRecord(getOptionDouble(cmd, "min-result-sim", ComparisonsStoreParameters.DEFAULT_MIN_SIMILARITY_TO_RECORD))
      .build();
  }

  public static RecursiveClusteringExtensionStrategyParameters buildExtensionStrategy(@NotNull CommandLine cmd) {
    return RecursiveClusteringExtensionStrategyParameters.builder()
      .sentMaxGap(getOptionInt(cmd, "max-gap", RecursiveClusteringExtensionStrategyParameters.DEFAULT_SENT_MAX_GAP))
      .minClusterSize(getOptionInt(cmd, "min-fragment-size", RecursiveClusteringExtensionStrategyParameters.DEFAULT_MIN_CLUSTER_SIZE))
      .minFragmentSideSim(getOptionDouble(cmd, "min-fragment-sim", RecursiveClusteringExtensionStrategyParameters.DEFAULT_MIN_FRAGMENT_SIDE_SIM))
      .build();
  }

  public static ShingleOverlapComparisonFilterParameters buildShingleComparatorParams(@NotNull CommandLine cmd) {
    return ShingleOverlapComparisonFilterParameters.builder()
      .size(getOptionInt(cmd, "shingle-size", ShingleOverlapComparisonFilterParameters.DEFAULT_SIZE))
      .overlap(getOptionInt(cmd, "shingle-overlap", ShingleOverlapComparisonFilterParameters.DEFAULT_OVERLAP))
      .plagiarizedLevel(getOptionDouble(cmd, "plagiarized-level", ShingleOverlapComparisonFilterParameters.DEFAULT_PLAGIARIZED_LEVEL))
      .discardLevel(getOptionDouble(cmd, "discard-level", ShingleOverlapComparisonFilterParameters.DEFAULT_DISCARD_LEVEL))
      .canBypassFiltering(cmd.hasOption("bypass-shingle-filter"))
      .build();
  }

  public static LengthComparisonFilterParameters buildLengthComparatorParams(@NotNull CommandLine cmd) {
    return LengthComparisonFilterParameters.builder()
      .canBypassFiltering(cmd.hasOption("bypass-length-filter"))
      .sentenceSizeRatioThreshold(getOptionDouble(cmd, "document-size-ratio-th", LengthComparisonFilterParameters.DEFAULT_SENTENCES_SIZE_RATIO_THRESHOLD))
      .build();
  }

  public static Pipeline.PipelineParameters buildPipelineParameters(@NotNull CommandLine cmd) {
    return PipelineParameters.builder()
      .canBypassTemplateRemoval(cmd.hasOption("bypass-template-removal"))
      .build();
  }

  public static IndexBasedWordAlignmentSeedingStrategyParameters buildSeedingParams(@NotNull CommandLine cmd) {
    return IndexBasedWordAlignmentSeedingStrategyParameters.builder()
      .sizeRatioThreshold(getOptionFloat(cmd, "sentence-size-ratio-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_SIZE_RATIO_THRESHOLD))
      .alignmentSimilarityThreshold(getOptionFloat(cmd, "alignment-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_ALIGNMENT_SIMILARITY_THRESHOLD))
      .vectorSimilarityThreshold(getOptionFloat(cmd, "vector-sim-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_VECTOR_SIMILARITY_THRESHOLD))
      .minSentencesToConsider(getOptionInt(cmd, "min-sentences-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_MIN_SENTENCES_TO_CONSIDER))
      .portionOfSentencesToConsider(getOptionDouble(cmd, "sentences-percentage-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER))
      .maxPortionOfSentencesToConsiderMultiSearch(getOptionDouble(cmd, "sentences-percentage-multi-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_MAX_PORTION_OF_SENTENCES_TO_CONSIDER_MULTI_SEARCH))
      .sentencesPerDocumentToConsiderMultiSearch(getOptionInt(cmd, "sentences-per-document-th", IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_SENTENCES_PER_DOCUMENT_TO_CONSIDER_MULTI_SEARCH))
      .build();
  }

  public static LuceneFileDataSourceParameters buildLuceneParams(@NotNull CommandLine cmd) {
    return LuceneFileDataSourceParameters.builder()
      .similarDocumentsLimit(getOptionInt(cmd, "max-sim-docs", LuceneFileDataSourceParameters.DEFAULT_SIMILAR_DOCUMENTS_LIMIT))
      .maxMetadataSimilarDoc(getOptionInt(cmd, "max-meta-docs", LuceneFileDataSourceParameters.DEFAULT_MAX_META_SIMILAR_DOC))
      .build();
  }

  public static MetadataComparatorParameters buildMetadataParams(@NotNull CommandLine cmd) {
    var hasBypassOption = cmd.hasOption("enable-metadata-filter");
    return MetadataComparatorParameters.builder()
      .canBypassFiltering(!hasBypassOption)
      .build();
  }
}
