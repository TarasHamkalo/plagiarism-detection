package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.ResultsFilter.ResultsFilterParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator.SentenceComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator.ShingleComparatorParameters;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource.SimilarDocumentSourceParameters;
import dev.taras.hamkalo.plagdet.io.reporting.ZipAnalysisReportWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

      String oldPath = cmd.getOptionValue("o");
      String newPath = cmd.getOptionValue("n");
      String templatePath = cmd.getOptionValue("t");
      String lucenePath = cmd.getOptionValue("l");
      boolean indexOnly = cmd.hasOption("i");

      SentenceComparatorParameters sentenceParams = buildSentenceComparatorParams(cmd);
      ShingleComparatorParameters shingleParams = buildShingleComparatorParams(cmd);
      MetadataComparatorParameters metadataParams = MetadataComparatorParameters.builder()
        .bypassFiltering(cmd.hasOption("bypass-metadata-filter"))
        .build();

      SimilarDocumentSourceParameters similarDocParams = buildSimilarDocumentSourceParams(cmd);
      ResultsFilterParameters resultsFilterParameters = buildResultsFilterParams(cmd);

      PlagdetParameters plagdetParameters = PlagdetParameters.builder()
        .luceneIndexDirectoryPath(lucenePath)
        .templateDocumentPath(templatePath)
        .sentenceComparatorParameters(sentenceParams)
        .shingleComparatorParameters(shingleParams)
        .metadataComparatorParameters(metadataParams)
        .similarDocumentSourceParameters(similarDocParams)
        .resultsFilterParameters(resultsFilterParameters)
        .build();

      run(newPath, oldPath, plagdetParameters, indexOnly);
    } catch (ParseException e) {
      log.error("Error parsing command-line arguments: {}", e.getMessage());
      formatter.printHelp("java -jar plagdet.jar", options);
    }
  }

  private static ResultsFilterParameters buildResultsFilterParams(CommandLine cmd) {
    return ResultsFilterParameters.builder()
      .bypassResultsFiltering(cmd.hasOption("bypass-results-filter"))
      .maxResultsPerDocument(getOptionInt(cmd, "max-results", ResultsFilterParameters.DEFAULT_MAX_RESULTS_PER_DOCUMENT) )
      .minSimilarityToInclude(getOptionDouble(cmd, "min-similarity", ResultsFilterParameters.DEFAULT_MIN_SIMILARITY_TO_INCLUDE))
      .build();
  }

  private static @NotNull Options getOptions() {
    Options options = new Options();

    options.addRequiredOption("n", "new-path", true, "Path to the new submissions zip/directory");
    options.addOption("l", "lucene-path", true, "Path to the lucene index directory, if empty, then temporary directory created");
    options.addOption("o", "old-path", true, "Path to the old submissions directory (optional)");
    options.addOption("t", "template-path", true, "Path to the template file (optional)");
    options.addOption("i", "index-only", false, "Flag to index submissions without comparison");
    options.addOption("h", "help", false, "Print help information");

    options.addOption("bypass-metadata-filter", false, "Whether to stop comparison after matching authors/modifier");
    options.addOption("bypass-shingle-filter", false, "Whether to stop comparison after shingle overlap threshold reached");

    options.addOption("max-gap", true, "Maximum sentence gap for merging");
    options.addOption("sentence-sim", true, "Sentence vector similarity threshold to be considered similar");
    options.addOption("min-cluster-size", true, "Minimum number of sentence to be considered as plagiarism");
    options.addOption("min-fragment-sim", true, "Min similarity for a group of sentence to be considered as similar");
    options.addOption("min-fragment-length", true, "Minimum fragment character length");

    options.addOption("shingle-size", true, "Shingle size");
    options.addOption("shingle-overlap", true, "Shingle overlap");
    options.addOption("plagiarized-level", true, "Threshold for Jaccard similarity, over which two are considered plagiarized");
    options.addOption("discard-level", true, "Threshold for Jaccard similarity, document pairs below that threshold will be considered as plagiarism free");

    options.addOption("max-results", true, "Maximum number of pairs/comparisons per document to be stored in result");
    options.addOption("min-similarity", true, "Minimum similarity for comparison to be included in result");
    options.addOption("bypass-results-filter", "No limit for results count which would be equal to (files_count * (files_count - 1) / 2) + files_count * similar-docs-limit");

    options.addOption("similar-docs-limit", true, "Limit number of indexed documents retrieved for each submission document");
    return options;
  }

  private static void run(
    @NotNull String inputDir,
    @Nullable String oldSubmissionsDir,
    @NotNull PlagdetParameters parameters,
    boolean indexOnly
  ) {
      System.out.println("Indexing with the following parameters:");
      System.out.println("New Submissions Directory: " + inputDir);
      System.out.println("Old Submissions Directory: " + oldSubmissionsDir);
      System.out.println("Template Document: " + parameters.getTemplateDocumentPath());
      System.out.println("Sentence Parameters: " + parameters.getSentenceComparatorParameters());
      System.out.println("Shingle Parameters: " + parameters.getShingleComparatorParameters());
      System.out.println("Metadata Parameters: " + parameters.getMetadataComparatorParameters());
      System.out.println("Similar Document Parameters: " + parameters.getSimilarDocumentSourceParameters());

      Plagdet plagdet = new Plagdet(parameters);
      if (indexOnly) {
        index(inputDir, plagdet);
        return;
      }

      if (oldSubmissionsDir != null) {
        plagdet.index(oldSubmissionsDir);
      }

      var report = plagdet.compare(inputDir);
      ZipAnalysisReportWriter analysisReportWriter = new ZipAnalysisReportWriter();
      analysisReportWriter.writeReport(report);

  }

  private static void index(
    @NotNull String inputDir,
    @NotNull Plagdet plagdet
  ) {
    System.out.printf("Indexing directory %s\n", inputDir);
    plagdet.index(inputDir);
    System.out.printf("Indexing done, index written to dir %s\n", plagdet.getLuceneIndex().getIndexDirectoryPath());
  }

  private static SentenceComparatorParameters buildSentenceComparatorParams(CommandLine cmd) {
    return SentenceComparatorParameters.builder()
      .maxGap(getOptionInt(cmd, "max-gap", SentenceComparatorParameters.DEFAULT_MAX_GAP))
      .sentenceVectorSimilarityThreshold(
        getOptionDouble(
          cmd, "sentence-sim", SentenceComparatorParameters.DEFAULT_SENTENCE_VECTOR_SIMILARITY_THRESHOLD)
      )
      .minClusterSize(getOptionInt(cmd, "min-cluster-size", SentenceComparatorParameters.DEFAULT_MIN_CLUSTER_SIZE))
      .minFragmentSideSim(getOptionDouble(cmd, "min-fragment-sim", SentenceComparatorParameters.DEFAULT_MIN_FRAGMENT_SIDE_SIM))
      .minFragmentCharLength(getOptionInt(cmd, "min-fragment-length", SentenceComparatorParameters.DEFAULT_MIN_FRAGMENT_CHAR_LENGTH))
      .build();
  }

  private static ShingleComparatorParameters buildShingleComparatorParams(CommandLine cmd) {
    return ShingleComparatorParameters.builder()
      .size(getOptionInt(cmd, "shingle-size", ShingleComparatorParameters.DEFAULT_SIZE))
      .overlap(getOptionInt(cmd, "shingle-overlap", ShingleComparatorParameters.DEFAULT_OVERLAP))
      .plagiarizedLevel(getOptionDouble(cmd, "plagiarized-level", ShingleComparatorParameters.DEFAULT_PLAGIARIZED_LEVEL))
      .discardLevel(getOptionDouble(cmd, "discard-level", ShingleComparatorParameters.DEFAULT_DISCARD_LEVEL))
      .bypassFiltering(cmd.hasOption("bypass-shingle-filter"))
      .build();
  }


  private static SimilarDocumentSourceParameters buildSimilarDocumentSourceParams(CommandLine cmd) {
    return SimilarDocumentSourceParameters.builder()
      .similarDocumentsLimit(
        getOptionInt(cmd, "similar-docs-limit", SimilarDocumentSourceParameters.DEFAULT_SIMILAR_DOCUMENTS_LIMIT)
      )
      .build();
  }

  private static int getOptionInt(CommandLine cmd, String option, int defaultValue) {
    return Integer.parseInt(cmd.getOptionValue(option, String.valueOf(defaultValue)));
  }

  private static double getOptionDouble(CommandLine cmd, String option, double defaultValue) {
    return Double.parseDouble(cmd.getOptionValue(option, String.valueOf(defaultValue)));
  }
}
