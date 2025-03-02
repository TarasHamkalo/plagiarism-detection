package dev.taras.hamkalo.plagdet.io.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileReportWriter {

  public static final String OVERVIEW_NAME = "overview.json";
  public static final String PAIRS_DIR = "pairs";
  public static final String FILES_DIR = "files";
  public static final String INDEXED_FILES_DIR = "files/indexed";

  @NotNull Boolean removeFilesOnExit;

  ObjectMapper mapper = new ObjectMapper();

  public void writeReport(@NotNull Report report, @NotNull Path destination) {
    try {
      writeOverview(report, destination);
      writeSubmittedDocuments(report.getSubmissions(), destination);
      writeIndexedDocuments(report.getDocumentsRetrievedFromIndex(), destination);
      writeDocumentPairs(report.getPairs(), destination);
    } catch (IOException e) {
      throw new PlagdetException("Could not write zip report", e);
    }
  }

  public void writeOverview(
    @NotNull Report report,
    @NotNull Path destination
  )  {
    Path overviewPath = IoUtils.resolveFile(destination, OVERVIEW_NAME, removeFilesOnExit);
    log.trace("Wrote overview to {}", overviewPath);
    writeObject(overviewPath, report);
  }

  public void writeDocumentPairs(
    @NotNull List<DocumentPair> documentPairs,
    @NotNull Path destination
  ) throws IOException {
    Path pairsDir = IoUtils.resolveDirectory(destination, PAIRS_DIR, removeFilesOnExit);
    for (DocumentPair documentPair : documentPairs) {
      Path path = IoUtils.resolveFile(pairsDir, documentPair.getId() + ".json", removeFilesOnExit);
      writeObject(path, documentPair);
      log.trace("Wrote document pair to: {}", path);
    }
  }

  public void writeSubmittedDocuments(
    @NotNull List<Submission> submissions,
    @NotNull Path destination
  ) throws IOException {
    Path filesDir = IoUtils.resolveDirectory(destination, FILES_DIR, removeFilesOnExit);
    for (Submission submission : submissions) {
      String baseName = FilenameUtils.getBaseName(submission.getPath().toString());
      Path submissionDir = IoUtils.resolveDirectory(filesDir, baseName, removeFilesOnExit);
      writeDocuments(submission.getDocuments(), submissionDir);
    }
  }

  public void writeDocuments(
    @NotNull List<DocumentEntry> documentEntries,
    @NotNull Path destination
  ) throws IOException {
    for (var documentEntry : documentEntries) {
      Path contentFilePath = IoUtils.resolveFile(
        destination, documentEntry.getId() + "_content.txt", removeFilesOnExit
      );
      Files.writeString(contentFilePath, documentEntry.getFileData().getContent());

      Path metadataFilePath = IoUtils.resolveFile(
        destination, documentEntry.getId() + "_meta.json", removeFilesOnExit
      );
      writeObject(metadataFilePath, documentEntry);
      log.trace("Wrote document entry {}", documentEntry.getFileData().getPersistentPath());
    }
  }

  public void writeIndexedDocuments(
    @NotNull List<DocumentEntry> documentEntries,
    @NotNull Path destination
  ) throws IOException {
    Path indexedPath = IoUtils.resolveDirectory(destination, INDEXED_FILES_DIR, removeFilesOnExit);
    writeDocuments(documentEntries, indexedPath);
  }

  private <T> void writeObject(@NotNull Path destination, @NotNull T object) {
    try {
      mapper.writerFor(object.getClass()).writeValue(destination.toFile(), object);
    } catch (IOException e) {
      log.warn("Failed to create JSON mapping: {}", e.getMessage());
    }
  }
}
