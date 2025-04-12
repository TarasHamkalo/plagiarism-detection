package dev.taras.hamkalo.plagdet.io.report;

import dev.taras.hamkalo.plagdet.ComparisonsStore;
import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
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

import static dev.taras.hamkalo.plagdet.io.report.FileReportWriter.FILES_DIR;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BufferedReportWriter {

  /**
   * Represents a temporary directory to which results are flushed before being archived
   */
  @NotNull Path outputDirectory;

  /**
   * Serves as cache and filter for comparisons
   */
  @NotNull ComparisonsStore resultsStore;

  @NotNull FileReportWriter fileReportWriter;

  public BufferedReportWriter(
    @NotNull ComparisonsStore comparisonsStore
  ) throws IOException {
    this.resultsStore = comparisonsStore;
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

  public void writeComparison(@NotNull Comparison comparison) {
    resultsStore.recordComparison(comparison);
  }

  public void flushDocument(@NotNull DocumentEntry documentEntry) throws ReportWriteException {
    String submitterDirectoryName = documentEntry.getFileData().getPersistentPath().split("/")[1]; // :)

    try {

      Path filesDir = IoUtils.resolveDirectory(outputDirectory, FILES_DIR, true);

      Path submitterDirectory =
        IoUtils.resolveDirectory(filesDir, submitterDirectoryName, true);

      var comparisons = resultsStore.removeComparisonsByDocumentId(documentEntry.getId());
      if (!comparisons.isEmpty()) {
        fileReportWriter.writeComparisons(comparisons, outputDirectory);
      }

      fileReportWriter.writeDocument(documentEntry, comparisons, submitterDirectory);
    } catch (IOException e) {
      throw new ReportWriteException("Could not write document entry", e);
    }

  }
}
