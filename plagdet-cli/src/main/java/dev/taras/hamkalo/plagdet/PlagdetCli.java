package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.ComparisonsStore.ComparisonsStoreParameters;
import dev.taras.hamkalo.plagdet.comparision.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy.LengthBasedFilteringStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy.RecursiveClusteringExtensionStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter.LengthComparisonFilterParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneFileDataSource.LuceneFileDataSourceParameters;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline.PipelineParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class PlagdetCli {

  public static void run(String[] args) {
    Options options = getOptions();

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("h")) {
        formatter.printHelp("java -jar plagdet.jar", options);
        return;
      }

      String newPath = cmd.getOptionValue("n");
      String templatePath = cmd.getOptionValue("t");
      String lucenePath = cmd.getOptionValue("l");
      if (newPath == null || !Files.exists(Path.of(newPath))) {
        log.error("New submissions path does not exist, {}", newPath);
        return;
      }

      if (templatePath != null && !Files.exists(Path.of(templatePath))) {
        log.error("Template path does not exist, {}", templatePath);
        return;
      }

      boolean indexOnly = cmd.hasOption("i");
      var params = PlagdetParameters.builder()
        .reportFilePath(cmd.getOptionValue("output-path", "report.zip"))
        .templateDocumentPath(templatePath)
        .luceneIndexDirectoryPath(lucenePath)
        .filteringStrategyParameters(OptionsMapper.buildFilteringStrategyParameters(cmd))
        .extensionStrategyParameters(OptionsMapper.buildExtensionStrategy(cmd))
        .seedingStrategyParameters(OptionsMapper.buildSeedingParams(cmd))
        .pipelineParameters(OptionsMapper.buildPipelineParameters(cmd))
        .shingleOverlapComparisonFilterParameters(OptionsMapper.buildShingleComparatorParams(cmd))
        .lengthComparisonFilterParameters(OptionsMapper.buildLengthComparatorParams(cmd))
        .comparisonsStoreParameters(OptionsMapper.buildComparisonsStoreParameters(cmd))
        .luceneFileDataSourceParameters(OptionsMapper.buildLuceneParams(cmd))
        .metadataComparatorParameters(OptionsMapper.buildMetadataParams(cmd))
        .build();
      run(newPath, params, indexOnly);

    } catch (ParseException e) {
      log.error("Error parsing command-line arguments: {}", e.getMessage());
      formatter.printHelp("java -jar plagdet.jar", options);
    }
  }

  private static @NotNull Options getOptions() {
    return OptionsBuilder.builder()
      .qualifiedRequired(
        "n", "new-path", "Path to the new submissions zip/directory", null
      )
      .qualified(
        "l", "lucene-path", "Path to the lucene index directory, if empty, then temporary directory created", null
      )
      .qualified("t", "template-path", "Path to the template file", "IPC task description")
      .qualified(
        "o", "output-path", "Path to report file, to which report data will be written", "report.zip"
      )
      .noArg("i", "Flag to index submissions without comparison", "false")
      .noArg("h", "Print help information", null)
      .noArg(
        "enable-metadata-filter",
        "Flag to stop comparison after matching authors/modifier",
        MetadataComparatorParameters.DEFAULT_BYPASS_FILTERING
      )
      .noArg(
        "bypass-spellcheck",
        "Flag to stop executing spell checking",
        PipelineParameters.DEFAULT_BYPASS_SPELLCHECK
      )
      .noArg(
        "bypass-results-filter",
        "Flag to store all results produced during comparison",
        ComparisonsStoreParameters.DEFAULT_BYPASS_FILTERING
      )
      .noArg(
        "bypass-shingle-filter",
        "Flag to continue comparison after shingle overlap threshold reached.",
        ShingleOverlapComparisonFilterParameters.DEFAULT_BYPASS_FILTERING
      )
      .noArg(
        "bypass-length-filter",
        "Flag to continue comparison after size ratio wasn't matched.",
        LengthComparisonFilterParameters.DEFAULT_BYPASS_FILTERING
      )
      .noArg(
        "bypass-template-removal",
        "Flag to stop removing template document matches from [new-path] document collection.",
        PipelineParameters.DEFAULT_BYPASS_TEMPLATE_REMOVAL
      )
      .optional(
        "min-char-len",
        "Minimum character length for fragment to be preserved as plagiarism case.",
        LengthBasedFilteringStrategyParameters.DEFAULT_MIN_CHARACTER_LENGTH
      )
      .optional(
        "max-gap",
        "Gap between two matches which is acceptable during match merging.",
        RecursiveClusteringExtensionStrategyParameters.DEFAULT_SENT_MAX_GAP
      )
      .optional(
        "min-fragment-size",
        "Minimum number of sentence to be considered as plagiarism.",
        RecursiveClusteringExtensionStrategyParameters.DEFAULT_MIN_CLUSTER_SIZE
      )
      .optional(
        "min-fragment-sim",
        "Min similarity for a group of sentence to be considered as similar, otherwise group is resplit.",
        RecursiveClusteringExtensionStrategyParameters.DEFAULT_MIN_FRAGMENT_SIDE_SIM
      )
      .optional(
        "sentence-size-ratio-th",
        "Drop every sentence match below that size ratio min(len1 / len2, len2 / len1).",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_SIZE_RATIO_THRESHOLD
      )
      .optional(
        "alignment-th",
        "Drop every sentence match below that similarity by Word alignment score.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_ALIGNMENT_SIMILARITY_THRESHOLD
      )
      .optional(
        "vector-sim-th",
        "Query HNSW sentence index for top K sentences above that threshold.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_VECTOR_SIMILARITY_THRESHOLD
      )
      .optional(
        "min-sentences-th",
        "Represents the least amount of top sentences to query for during Index search." +
        " The higher value the more sentences suspicious sentence is compared to," +
        " eliminating false negatives during lookup stage.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_MIN_SENTENCES_TO_CONSIDER
      )
      .optional(
        "sentences-percentage-th",
        "Percentage of the number of source document sentences that will be considered during index search.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_PORTION_OF_SENTENCES_TO_CONSIDER
      )
      .optional(
        "sentences-percentage-multi-th",
        "Represents the hard limit in percentage of total sentences " +
        "for count of top sentences to query for, when doing document to corpus match.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_MAX_PORTION_OF_SENTENCES_TO_CONSIDER_MULTI_SEARCH
      )
      .optional(
        "sentences-per-document-th",
        "Represents default per document count of top sentences to query for" +
        "e.g. having 10 documents in corpus and th=5, for each sentence will be queried 10 * 5 top sentences from index.",
        IndexBasedWordAlignmentSeedingStrategyParameters.DEFAULT_SENTENCES_PER_DOCUMENT_TO_CONSIDER_MULTI_SEARCH
      )
      .optional(
        "shingle-size",
        "Shingle size.",
        ShingleOverlapComparisonFilterParameters.DEFAULT_SIZE
      )
      .optional(
        "shingle-overlap",
        "Shingle overlap.",
        ShingleOverlapComparisonFilterParameters.DEFAULT_OVERLAP
      )
      .optional(
        "plagiarized-level",
        "Threshold for Jaccard similarity, over which two are considered plagiarized.",
        ShingleOverlapComparisonFilterParameters.DEFAULT_PLAGIARIZED_LEVEL
      )
      .optional(
        "discard-level",
        "Threshold for Jaccard similarity, document pairs below that " +
        "threshold will be considered as plagiarism free.",
        ShingleOverlapComparisonFilterParameters.DEFAULT_DISCARD_LEVEL
      )
      .optional(
        "max-results",
        "Maximum number of comparisons per document to be stored in report.",
        ComparisonsStoreParameters.DEFAULT_MAX_RESULTS_PER_DOCUMENT
      )
      .optional(
        "min-result-sim",
        "Minimum similarity for comparison to be recorded.",
        ComparisonsStoreParameters.DEFAULT_MIN_SIMILARITY_TO_RECORD
      )
      .optional(
        "max-meta-docs",
        "Amount of queried documents per filed with same metadata",
        LuceneFileDataSourceParameters.DEFAULT_MAX_META_SIMILAR_DOC
      )
      .optional(
        "max-sim-docs",
        "Amount of content queried documents",
        LuceneFileDataSourceParameters.DEFAULT_SIMILAR_DOCUMENTS_LIMIT
      )
      .noArg(
        "document-size-ratio-th",
        "Document pairs for which size ratio is lower than specified value, won't be compared",
        LengthComparisonFilterParameters.DEFAULT_SENTENCES_SIZE_RATIO_THRESHOLD
      )
      .build();
  }

  private static void run(
    @NotNull String inputDir,
    @NotNull PlagdetParameters parameters,
    boolean indexOnly
  ) {
    System.out.println("Starting...");
    Plagdet plagdet = new Plagdet(parameters);
    if (indexOnly) {
      index(inputDir, plagdet);
      return;
    }

    plagdet.compare(inputDir);
  }

  private static void index(
    @NotNull String inputDir,
    @NotNull Plagdet plagdet
  ) {
    System.out.printf("Indexing directory %s\n", inputDir);
    plagdet.index(inputDir);
    System.out.printf(
      "Index written to dir %s\n", plagdet.getLuceneIndex().getIndexDirectoryPath()
    );
  }
}
