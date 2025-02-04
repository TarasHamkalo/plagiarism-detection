package dev.taras.hamkalo.plagdet.io.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.io.FileData;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class ZipAnalysisReportWriter {

  public static final String REPORT_NAME = "report.zip";

  /**
   * Generate zip report
   * report.zip
   * /files -> txt files of compared files content, and meta files in json
   * /pairs -> a.b.json comparison results
   */
  public void writeReport(@NotNull Report report) {
    Path tempDir = null;
    try {

      tempDir = Files.createTempDirectory("zip_report");
      tempDir.toFile().deleteOnExit();

      log.debug("Created temp directory: {}", tempDir);
      writeOverview(report, tempDir);
      writeSubmittedDocuments(report.getSubmissions(), tempDir);
      writeIndexedDocuments(report.getDocumentsRetrievedFromIndex(), tempDir);
      writeDocumentPairs(report.getPairs(), tempDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final Path finalTempDir = tempDir;
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

  private void writeOverview(
    @NotNull Report report,
    @NotNull Path tempDir
  ) throws IOException {
    Path overviewPath = tempDir.resolve("overview.json");
    String overviewJson = analysisOverviewJsonString(report);
    log.debug("Writing overview to {}", overviewPath);
    Files.writeString(overviewPath, overviewJson);
    overviewPath.toFile().deleteOnExit();
  }

  private String analysisOverviewJsonString(@NotNull Report report) {
    final Map<String, Object> result = new HashMap<>();
    final String baseName = FilenameUtils.getBaseName(report.getInputPath());
    final String fullName = FilenameUtils.getName(report.getInputPath());

    List<DocumentEntry> documents = report.getSubmissions().stream()
      .flatMap(submission -> submission.getDocuments().stream())
      .collect(Collectors.toList());

    Map<String, Integer> extensionToCount = documents.stream()
      .collect(Collectors.toMap((d) -> d.getFileData().getExtension(), d -> 1, (p1, p2) -> p1 + 1));

    result.put("name", baseName);
    result.put("analyzedFileName", fullName);
    result.put("dateAnalyzed", Date.from(Instant.now()));
    result.put("numberOfComparisons", report.getComparisonsCount());
    result.put("docxFiles", extensionToCount.getOrDefault("docx", 0));
    result.put("docFiles", extensionToCount.getOrDefault("doc", 0));
    result.put("pdfFiles", extensionToCount.getOrDefault("pdf", 0));
    result.put("totalFilesCount", documents.size());
    result.put("status", "success");
    result.put("configuration", report.getPlagdetParameters());
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      log.warn("Failed to create json mapping, cause {}", e.toString());
    }
    return "";
  }

  private void writeDocumentPairs(List<DocumentPair> documentPairs, Path tempDir) throws IOException {
    Path pairsDir = tempDir.resolve("pairs");
    Files.createDirectories(pairsDir);
    pairsDir.toFile().deleteOnExit();
    for (DocumentPair documentPair : documentPairs) {
      Path resultJson = pairsDir.resolve(
        documentPair.getId() + ".json"
      );
      String documentPairJson = mapDocumentPairToJson(documentPair);
      log.debug("Writing document pair to: {}", resultJson);
      Files.writeString(resultJson, documentPairJson);
      resultJson.toFile().deleteOnExit();
    }
  }

  private void writeSubmittedDocuments(List<Submission> submissions, Path tempDir) throws IOException {
    Path filesDir = tempDir.resolve("files");
    Files.createDirectories(filesDir);
    filesDir.toFile().deleteOnExit();
    for (Submission submission : submissions) {
      Path submissionDir = filesDir.resolve(FilenameUtils.getBaseName(submission.getPath().toString()));
      submissionDir.toFile().deleteOnExit();
      Files.createDirectories(submissionDir);
      writeDocuments(submission.getDocuments(), submissionDir);
    }
  }

  private void writeDocuments(List<DocumentEntry> documentEntries, Path outputDir) throws IOException {
    for (var documentEntry : documentEntries) {
      log.debug("Writing document {}", documentEntry.getFileData().getPersistentPath());
      Path contentFilePath = outputDir.resolve(documentEntry.getId() + "_content.txt");
      Files.writeString(contentFilePath, documentEntry.getFileData().getContent());
      contentFilePath.toFile().deleteOnExit();
      Path metadataFilePath = outputDir.resolve(documentEntry.getId() + "_meta.json");
      String metadataJson = mapDocumentEntryToJson(documentEntry);
      Files.writeString(metadataFilePath, metadataJson);
      metadataFilePath.toFile().deleteOnExit();
    }
  }

  private void writeIndexedDocuments(List<DocumentEntry> documentEntries, Path tempDir) throws IOException {
    Path indexedPath = tempDir.resolve("files/indexed");
    indexedPath.toFile().deleteOnExit();
    Files.createDirectories(indexedPath);
    writeDocuments(documentEntries, indexedPath);
  }


  private String mapDocumentPairToJson(DocumentPair documentPair) {
    Map<String, Object> result = new HashMap<>();
    result.put("id", documentPair.getId());
    result.put("firstId", documentPair.getFirst().getId());
    result.put("secondId", documentPair.getSecond().getId());
    result.put("source", documentPair.getPlagSource());
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
    List<String> pairIds = documentEntry.getDocumentPairs().stream()
      .map(p -> p.getId())
      .toList();

    final double maxSimilarity = documentEntry.getDocumentPairs().stream()
      .mapToDouble(p -> p.getMaxScore())
      .max()
      .orElse(0);

    result.put("id", documentEntry.getId());
//    var filePath = documentEntry.isIndexed() ?
//      fileData.getPersistentPath() : fileData.getTemporaryPath().toString();

    result.put("filename", FilenameUtils.getBaseName(fileData.getPersistentPath()));
    result.put("metadata", fileData.getMetadata());
    result.put("filepath", fileData.getPersistentPath());
    result.put("submitter", fileData.getSubmitter());
    result.put("indexed", documentEntry.isIndexed());
    result.put("pairIds", pairIds);
    result.put("maxSimilarity", maxSimilarity);

    var markings = new ArrayList<>(documentEntry.getTemplateMarking());
    markings.addAll(documentEntry.getCodeMarking());
    result.put("markings", markings);

    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      log.warn("Failed to create json mapping, cause {}", e.toString());
    }

    return "";
  }

}