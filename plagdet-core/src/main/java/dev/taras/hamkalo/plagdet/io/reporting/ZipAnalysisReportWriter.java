package dev.taras.hamkalo.plagdet.io.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.io.FileData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class ZipAnalysisReportWriter {

  public static final String REPORT_NAME = "report.zip";

  /**
   * Generate zip report
   * report.zip
   * /files -> txt files of compared files content
   * a.b.json comparison results
   */
  public void writeReport(List<Submission> submissions, List<DocumentPair> documentPairs) {

    Path tempDir = null;
    try {

      tempDir = Files.createTempDirectory("zip_report");
      log.debug("Created temp directory: {}", tempDir);

      writeDocuments(submissions, tempDir);
      writeDocumentPairs(documentPairs, tempDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Path finalTempDir = tempDir;
    try (OutputStream fos = new FileOutputStream(REPORT_NAME);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         ArchiveOutputStream<ZipArchiveEntry> aos = new ZipArchiveOutputStream(bos)) {
      Files.walk(tempDir).forEach(srcPath -> {
        if (!Files.isDirectory(srcPath)) {
          var entryName = srcPath.toString().replace(finalTempDir.toString(), "");
          ZipArchiveEntry ze = new ZipArchiveEntry(srcPath.toFile(), entryName);
          try {
            aos.putArchiveEntry(ze);
            aos.write(Files.readAllBytes(srcPath));
            aos.closeArchiveEntry();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeDocumentPairs(List<DocumentPair> documentPairs, Path tempDir) throws IOException {
    for (DocumentPair documentPair : documentPairs) {
      Path resultJson = tempDir.resolve(
        documentPair.getFirst().getId() + "_" + documentPair.getSecond().getId() + ".json"
      );
      String documentPairJson = mapDocumentPairToJson(documentPair);
      log.debug("Writing document pair to: {}", resultJson);
      Files.writeString(resultJson, documentPairJson);
    }
  }

  private void writeDocuments(List<Submission> submissions, Path tempDir) throws IOException {
    Path filesDir = tempDir.resolve("files");
    Files.createDirectories(filesDir);
    for (Submission submission : submissions) {
      Path submissionDir = filesDir.resolve(FilenameUtils.getBaseName(submission.getPath().toString()));
      Files.createDirectories(submissionDir);

      for (var documentEntry : submission.getDocuments()) {
        log.debug("Writing document with id {}", documentEntry.getId());
        Path contentFilePath = submissionDir.resolve(documentEntry.getId() + "_content.txt");
        Files.writeString(contentFilePath, documentEntry.getFileData().getContent());
        Path metadataFilePath = submissionDir.resolve(documentEntry.getId() + "_meta.json");
        String metadataJson = mapDocumentEntryToJson(documentEntry);
        Files.writeString(metadataFilePath, metadataJson);
      }
    }
  }

  private String mapDocumentPairToJson(DocumentPair documentPair) {
    Map<String, Object> result = new HashMap<>();
    result.put("firstId", documentPair.getFirst().getId());
    result.put("secondId", documentPair.getSecond().getId());
    result.put("plagCases", documentPair.getPlagCases());
    result.put("plagScores", documentPair.getPlagScores());
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      log.warn("Failed to create json mapping, cause {}", e.toString());
    }
    return "";
  }

  private String mapDocumentEntryToJson(DocumentEntry documentEntry) {
    Map<String, Object> result = new HashMap<>();
    FileData fileData = documentEntry.getFileData();
    result.put("id", documentEntry.getId());

    result.put("filename", FilenameUtils.getBaseName(fileData.getPath().toString()));
    result.put("metadata", fileData.getMetadata());
    result.put("filepath", fileData.getPath());
    result.put("submitter", fileData.getSubmitter());

    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      log.warn("Failed to create json mapping, cause {}", e.toString());
    }

    return "";
  }

}