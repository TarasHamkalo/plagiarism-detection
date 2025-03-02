package dev.taras.hamkalo.plagdet.io.session;

import dev.taras.hamkalo.plagdet.Plagdet;
import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import dev.taras.hamkalo.plagdet.io.reporting.reader.FileReportReader;
import dev.taras.hamkalo.plagdet.io.reporting.writer.FileReportWriter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionManager {

  public static final String SESSION_DIRECTORY_NAME = "session.plagdet";

  FileReportWriter fileReportWriter = new FileReportWriter(false);

  FileReportReader fileReportReader = new FileReportReader();

  public static boolean hasSessionFile() {
    return Files.exists(Path.of(SESSION_DIRECTORY_NAME));
  }

  public void restoreSession(@NotNull Plagdet plagdet) {
    log.debug("Restoring session");
    var report = fileReportReader.readReport(Path.of(SESSION_DIRECTORY_NAME));

    plagdet.getSubmissions().addAll(report.getSubmissions());
    plagdet.getComparisonPipeline().getComparisonIgnore().addAll(report.getComparisonIgnore());
    plagdet.getComparisonPipeline().getComparisonResults().addAll(report.getPairs());

    report.getDocumentsRetrievedFromIndex().forEach(d ->
      SimilarDocumentSource.getRetrievedDocumentsCache().put(d.getId(), d)
    );
    try {
      FileUtils.deleteDirectory(Path.of(SESSION_DIRECTORY_NAME).toFile());
    } catch (IOException e) {
      throw new PlagdetException("Was not able to delete session directory", e);
    }
  }

  public void saveSession(@NotNull Plagdet plagdet) {
    log.debug("Saving session");
    var report = Report.from(plagdet, true);
    if (report != null) {
      report = report.withComparisonIgnore(
        plagdet.getComparisonPipeline().getComparisonIgnore()
      );
      try {
        var path = Files.createDirectory(Path.of(SESSION_DIRECTORY_NAME));
        fileReportWriter.writeReport(report, path);
      } catch (IOException e) {
        throw new PlagdetException("Was not able to create session directory");
      }
    }
  }
}
