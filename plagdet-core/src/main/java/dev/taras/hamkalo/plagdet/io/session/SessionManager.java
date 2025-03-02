package dev.taras.hamkalo.plagdet.io.session;

import dev.taras.hamkalo.plagdet.Plagdet;
import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.io.reporting.writer.FileReportWriter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionManager {

  public static final String SESSION_DIRECTORY_NAME = "session.plagdet";

  FileReportWriter fileReportWriter = new FileReportWriter(false);

  public static boolean hasSessionFile() {
    return Files.exists(Path.of(SESSION_DIRECTORY_NAME));
  }

  public void restoreSession() {
  }

  public void saveSession(@NotNull Plagdet plagdet) {
    var report = Report.from(plagdet);
    if (report != null) {
      report = report.withComparisonIgnore(
        plagdet.getComparisonPipeline().getComparisonIgnore()
      );

      fileReportWriter.writeReport(report, Path.of(SESSION_DIRECTORY_NAME));
    }
  }
}
