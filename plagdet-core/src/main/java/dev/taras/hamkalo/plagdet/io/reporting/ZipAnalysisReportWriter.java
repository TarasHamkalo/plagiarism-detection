package dev.taras.hamkalo.plagdet.io.reporting;

import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.io.mapping.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


@Slf4j
public class ZipAnalysisReportWriter {

  public static final String REPORT_NAME = "report.zip";
  public static final String OVERVIEW_NAME = "overview.json";
  public static final String PAIRS_DIR = "pairs";
  public static final String FILES_DIR = "files";
  public static final String INDEXED_FILES_DIR = "files/indexed";

  public void writeReport(@NotNull Report report) {
    try {
      Path tempDir = Files.createTempDirectory("zip_report");
      tempDir.toFile().deleteOnExit();

      log.debug("Created temp directory: {}", tempDir);
      writeOverview(report, tempDir);
      writeSubmittedDocuments(report.getSubmissions(), tempDir);
      writeIndexedDocuments(report.getDocumentsRetrievedFromIndex(), tempDir);
      writeDocumentPairs(report.getPairs(), tempDir);

      zipDirectory(tempDir);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private static void zipDirectory(Path tempDir) {
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
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeOverview(
    @NotNull Report report,
    @NotNull Path destination
  ) throws IOException {
    Path overviewPath = IoUtils.temporaryFile(destination, OVERVIEW_NAME);
    Files.writeString(overviewPath, JsonMapper.reportJson(report));
    log.trace("Wrote overview to {}", overviewPath);
  }

  private void writeDocumentPairs(
    @NotNull List<DocumentPair> documentPairs,
    @NotNull Path destination
  ) throws IOException {
    Path pairsDir = IoUtils.temporaryDirectory(destination, PAIRS_DIR);
    for (DocumentPair documentPair : documentPairs) {
      Path path = IoUtils.temporaryFile(pairsDir, documentPair.getId() + ".json");
      Files.writeString(path, JsonMapper.documentPairJson(documentPair));
      log.trace("Wrote document pair to: {}", path);
    }
  }

  private void writeSubmittedDocuments(
    @NotNull List<Submission> submissions,
    @NotNull Path destination
  ) throws IOException {
    Path filesDir = IoUtils.temporaryDirectory(destination, FILES_DIR);
    for (Submission submission : submissions) {
      String baseName = FilenameUtils.getBaseName(submission.getPath().toString());
      Path submissionDir = IoUtils.temporaryDirectory(filesDir, baseName);
      writeDocuments(submission.getDocuments(), submissionDir);
    }
  }

  private void writeDocuments(
    @NotNull List<DocumentEntry> documentEntries,
    @NotNull Path destination
  ) throws IOException {
    for (var documentEntry : documentEntries) {
      Path contentFilePath = IoUtils.temporaryFile(
        destination, documentEntry.getId() + "_content.txt"
      );
      Files.writeString(contentFilePath, documentEntry.getFileData().getContent());

      Path metadataFilePath = IoUtils.temporaryFile(
        destination, documentEntry.getId() + "_meta.json"
      );
      Files.writeString(metadataFilePath, JsonMapper.documentEntryJson(documentEntry));
      log.trace("Wrote document entry {}", documentEntry.getFileData().getPersistentPath());
    }
  }

  private void writeIndexedDocuments(
    @NotNull List<DocumentEntry> documentEntries,
    @NotNull Path destination
  ) throws IOException {
    Path indexedPath = IoUtils.temporaryDirectory(destination, INDEXED_FILES_DIR);
    writeDocuments(documentEntries, indexedPath);
  }

}