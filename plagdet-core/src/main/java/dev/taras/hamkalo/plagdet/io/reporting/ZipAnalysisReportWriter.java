package dev.taras.hamkalo.plagdet.io.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
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

import javax.swing.text.Document;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;


@Slf4j
public class ZipAnalysisReportWriter {

  public static final String REPORT_NAME = "report.zip";

  /**
   * Generate zip report
   * report.zip
   * /files -> txt files of compared files content
   * a.b.json comparison results
   */
  public void writeReport(List<Submission> submissions,
                          List<DocumentPair> documentPairs,
                          List<DocumentEntry> retrievedIndexedDocuments) {
    Path tempDir = null;
    try {

      tempDir = Files.createTempDirectory("zip_report");
      log.debug("Created temp directory: {}", tempDir);
      writeOverview(submissions, documentPairs, tempDir);
      writeSubmittedDocuments(submissions, tempDir);
      writeIndexedDocuments(retrievedIndexedDocuments, tempDir);
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

  private void writeOverview(List<Submission> submissions,
                             List<DocumentPair> documentPairs,
                             Path tempDir) throws IOException {
    Path overviewPath = tempDir.resolve("overview.json");
    String overviewJson = analysisOverviewJsonString(submissions, documentPairs);
    log.debug("Writing overview to {}", overviewPath);
    Files.writeString(overviewPath, overviewJson);
    analysisOverviewJsonString(submissions, documentPairs);
  }

  private String analysisOverviewJsonString(List<Submission> submissions, List<DocumentPair> documentPairs) {
    final Map<String, Object> result = new HashMap<>();
    final String analysisName = FilenameUtils.getBaseName(
      submissions.get(0).getPath().getParent().toString()
    );
    final long fileCount = submissions.stream()
      .mapToLong(submission -> submission.getDocuments().size())
      .sum();
    result.put("name", analysisName);
    result.put("analyzedFileName", analysisName);
    result.put("dateAnalyzed", Date.from(Instant.now()));
    result.put("numberOfComparisons", documentPairs.size());
    result.put("docxFiles", 0);
    result.put("docFiles", 0);
    result.put("pdfFiles", 0);
    result.put("totalFilesCount", fileCount);
    result.put("status", "success");
    result.put("configurationUsed", Collections.emptyList());
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
    for (DocumentPair documentPair : documentPairs) {
      Path resultJson = pairsDir.resolve(
        documentPairId(documentPair) + ".json"
      );
      String documentPairJson = mapDocumentPairToJson(documentPair);
      log.debug("Writing document pair to: {}", resultJson);
      Files.writeString(resultJson, documentPairJson);
    }
  }

  private @NotNull String documentPairId(DocumentPair documentPair) {
    return documentPair.getFirst().getId() + "_" + documentPair.getSecond().getId();
  }

  private void writeSubmittedDocuments(List<Submission> submissions, Path tempDir) throws IOException {
    Path filesDir = tempDir.resolve("files");
    Files.createDirectories(filesDir);
    for (Submission submission : submissions) {
      Path submissionDir = filesDir.resolve(FilenameUtils.getBaseName(submission.getPath().toString()));
      Files.createDirectories(submissionDir);
      writeDocuments(submission.getDocuments(), submissionDir);
    }
  }

  private void writeDocuments(List<DocumentEntry> documentEntries, Path outputDir) throws IOException {
    for (var documentEntry : documentEntries) {
      log.debug("Writing document with id {}", documentEntry.getId());
      Path contentFilePath = outputDir.resolve(documentEntry.getId() + "_content.txt");
      Files.writeString(contentFilePath, documentEntry.getFileData().getContent());
      Path metadataFilePath = outputDir.resolve(documentEntry.getId() + "_meta.json");
      String metadataJson = mapDocumentEntryToJson(documentEntry);
      Files.writeString(metadataFilePath, metadataJson);
    }
  }

  private void writeIndexedDocuments(List<DocumentEntry> documentEntries, Path tempDir) throws IOException {
    Path indexedPath = tempDir.resolve("files/indexed");
    Files.createDirectories(indexedPath);
    writeDocuments(documentEntries, indexedPath);
  }


  private String mapDocumentPairToJson(DocumentPair documentPair) {
    Map<String, Object> result = new HashMap<>();
    result.put("id", documentPairId(documentPair));
    result.put("firstId", documentPair.getFirst().getId());
    result.put("secondId", documentPair.getSecond().getId());
    result.put("direction", documentPair.getDirection());
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
      .map(this::documentPairId)
      .toList();

    final double maxSimilarity = documentEntry.getDocumentPairs().stream()
      .flatMap(p -> p.getPlagScores().stream())
      .mapToDouble(PlagScore::getScore)
      .max()
      .orElse(0);

    result.put("id", documentEntry.getId());

    result.put("filename", FilenameUtils.getBaseName(fileData.getPath().toString()));
    result.put("metadata", fileData.getMetadata());
    result.put("filepath", fileData.getPath());
    result.put("submitter", fileData.getSubmitter());
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