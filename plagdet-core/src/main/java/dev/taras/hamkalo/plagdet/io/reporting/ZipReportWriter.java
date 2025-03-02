package dev.taras.hamkalo.plagdet.io.reporting;

import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


@Slf4j
@RequiredArgsConstructor
public class ZipReportWriter {

  public static final String REPORT_NAME = "report.zip";

  FileReportWriter fileReportWriter = new FileReportWriter(true);

  public void writeReport(@NotNull Report report) {
    try {
      Path tempDir = Files.createTempDirectory("zip_report");
      tempDir.toFile().deleteOnExit();

      log.debug("Created temp directory: {}", tempDir);
      fileReportWriter.writeOverview(report, tempDir);
      fileReportWriter.writeSubmittedDocuments(report.getSubmissions(), tempDir);
      fileReportWriter.writeIndexedDocuments(report.getDocumentsRetrievedFromIndex(), tempDir);
      fileReportWriter.writeDocumentPairs(report.getPairs(), tempDir);
      zipDirectory(tempDir);

    } catch (IOException e) {
      throw new PlagdetException("Could not write zip report", e);
    }
  }

  private static void zipDirectory(Path tempDir) throws IOException {
    try (OutputStream fos = new FileOutputStream(REPORT_NAME);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         ArchiveOutputStream<ZipArchiveEntry> aos = new ZipArchiveOutputStream(bos)) {
      Files.walk(tempDir).forEach(srcPath -> {
        var entryName = tempDir.relativize(srcPath).toString();
        try {
          if (!Files.isDirectory(srcPath)) {
            ZipArchiveEntry ze = new ZipArchiveEntry(srcPath.toFile(), entryName);
            aos.putArchiveEntry(ze);
            aos.write(Files.readAllBytes(srcPath));
            aos.closeArchiveEntry();
          } else {
            ZipArchiveEntry dirEntry = new ZipArchiveEntry(
              entryName.endsWith("/") ? entryName : entryName + "/"
            );
            aos.putArchiveEntry(dirEntry);
            aos.closeArchiveEntry();
          }
        } catch (IOException e) {
          throw new PlagdetException("Could not create zip entry", e);
        }
      });
    }
  }
}