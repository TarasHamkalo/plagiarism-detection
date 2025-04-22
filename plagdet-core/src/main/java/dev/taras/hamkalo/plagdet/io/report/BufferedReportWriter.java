package dev.taras.hamkalo.plagdet.io.report;

import dev.taras.hamkalo.plagdet.ComparisonsStore;
import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.corpus.Corpus;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.io.ZipArchiver;
import dev.taras.hamkalo.plagdet.io.exception.ReportWriteException;
import dev.taras.hamkalo.plagdet.io.report.model.OverviewJsonModel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static dev.taras.hamkalo.plagdet.io.report.FileReportWriter.FILES_DIR;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BufferedReportWriter {

  /**
   * Represents a temporary directory to which results are flushed before being archived
   */
  @NotNull Path outputDirectory;

  /**
   * Stores comparison ids which were already written to disk, to prevent multiple writes
   */
  @NotNull Set<String> flushedComparisonIds;

  /**
   * Stores flushed indexed document ids
   */
  @NotNull Set<Integer> flushedIndexedDocumentsIds;

  /**
   * Serves as cache and filter for comparisons
   */
  @NotNull ComparisonsStore resultsStore;


  @NotNull FileReportWriter fileReportWriter;

  public BufferedReportWriter(
    @NotNull ComparisonsStore comparisonsStore
  ) throws IOException {
    this.resultsStore = comparisonsStore;
    this.flushedComparisonIds = new HashSet<>();
    this.flushedIndexedDocumentsIds = new HashSet<>();
    this.fileReportWriter = new FileReportWriter(true);
    this.outputDirectory = Files.createTempDirectory("plagdet_report_files_");
    outputDirectory.toFile().deleteOnExit();
  }

  public void createReport(@NotNull OverviewJsonModel overviewJsonModel) {
    try {
      fileReportWriter.writeOverview(overviewJsonModel, outputDirectory);
      ZipArchiver.zipDirectory(outputDirectory);
    } catch (IOException e) {
      throw new ReportWriteException("Could not create zip report", e);
    }
  }

  public void writeComparison(@NotNull Comparison comparison, boolean isSecondIndexed) {
    if (isSecondIndexed) {
      resultsStore.recordIndexBasedComparison(comparison);
    } else {
      resultsStore.recordComparison(comparison);
    }
  }

  public void flushDocumentContent(
    @NotNull DocumentEntry documentEntry
  ) {
    String submitterDirectoryName = documentEntry.getFileData().getPersistentPath().split("/")[1]; // :)
    try {
      Path filesDir = IoUtils.resolveDirectory(outputDirectory, FILES_DIR, true);
      Path submitterDirectory =
        IoUtils.resolveDirectory(filesDir, submitterDirectoryName, true);
      fileReportWriter.writeDocumentContent(documentEntry, submitterDirectory);
    } catch (IOException e) {
      throw new ReportWriteException("Could not write document's content", e);
    }
  }

  public void flushDocumentMetadata(
    @NotNull DocumentEntry documentEntry,
    @NotNull Corpus indexedDocumentsCorpus
  ) throws ReportWriteException {
    String submitterDirectoryName = documentEntry.getFileData().getPersistentPath().split("/")[1]; // :)
    try {
      Path filesDir = IoUtils.resolveDirectory(outputDirectory, FILES_DIR, true);
      Path submitterDirectory =
        IoUtils.resolveDirectory(filesDir, submitterDirectoryName, true);

      var indexBasedComparisons =
        resultsStore.removeIndexBasedComparisonsByDocumentId(documentEntry.getId());

      var deduplicatedComparisons =
        new ArrayList<>(
          resultsStore.removeComparisonsByDocumentId(documentEntry.getId()).stream()
            .filter(c -> !this.flushedComparisonIds.contains(c.getId()))
            .toList()
        );

      deduplicatedComparisons.addAll(indexBasedComparisons);

      if (!deduplicatedComparisons.isEmpty()) {
        fileReportWriter.writeComparisons(deduplicatedComparisons, outputDirectory);
      }

      fileReportWriter.writeDocumentMetadata(
        documentEntry, deduplicatedComparisons, submitterDirectory
      );
      var indexedDocumentsMap = indexedDocumentsCorpus.getDocumentsMap();
      var indexedDocuments = new ArrayList<DocumentEntry>();
      for (var indexBasedComparison : indexBasedComparisons) {
        int documentId = indexBasedComparison.getSecondId();
        if (!flushedIndexedDocumentsIds.contains(documentId)) {
          indexedDocuments.add(indexedDocumentsMap.get(documentId));
          flushedIndexedDocumentsIds.add(documentId);
        }
      }

      fileReportWriter.writeIndexedDocuments(indexedDocuments, outputDirectory);
      deduplicatedComparisons.forEach(c -> this.flushedComparisonIds.add(c.getId()));
    } catch (IOException e) {
      throw new ReportWriteException(
        "Could not write document's metadata or dependent documents", e
      );
    }

  }
}
