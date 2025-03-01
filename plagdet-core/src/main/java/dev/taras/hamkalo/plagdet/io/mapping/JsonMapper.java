package dev.taras.hamkalo.plagdet.io.mapping;

import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.io.FileData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.time.Instant;
import java.util.stream.Collectors;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class JsonMapper {

  public static String reportJson(@NotNull Report report) {
    final String baseName = FilenameUtils.getBaseName(report.getInputPath());
    final String fullName = FilenameUtils.getName(report.getInputPath());

    List<DocumentEntry> documents = report.getSubmissions().stream()
      .flatMap(submission -> submission.getDocuments().stream())
      .toList();

    Map<String, Long> extensionToCount = documents.stream()
      .map(d -> d.getFileData().getExtension())
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    Map<String, Object> properties = Map.of(
      "name", baseName,
      "analyzedFileName", fullName,
      "dateAnalyzed", Date.from(Instant.now()),
      "numberOfComparisons", report.getComparisonsCount(),
      "docxFiles", extensionToCount.getOrDefault("docx", 0L),
      "docFiles", extensionToCount.getOrDefault("doc", 0L),
      "pdfFiles", extensionToCount.getOrDefault("pdf", 0L),
      "totalFilesCount", documents.size(),
      "status", "success",
      "configuration", report.getPlagdetParameters()
    );

    return toJsonString(properties);
  }

  public static String documentPairJson(DocumentPair documentPair) {
    Map<String, Object> properties = Map.of(
      "id", documentPair.getId(),
      "firstId", documentPair.getFirst().getId(),
      "secondId", documentPair.getSecond().getId(),
      "source", documentPair.getPlagSource(),
      "plagCases", documentPair.getPlagCases(),
      "plagScores", documentPair.getPlagScores()
    );

    return toJsonString(properties);
  }

  public static String documentEntryJson(DocumentEntry documentEntry) {
    final FileData fileData = documentEntry.getFileData();
    final List<String> pairIds = documentEntry.getDocumentPairs().stream()
      .map(p -> p.getId())
      .toList();

    final double maxSimilarity = documentEntry.getDocumentPairs().stream()
      .mapToDouble(p -> p.getMaxScore())
      .max()
      .orElse(0);

    final var markings = new ArrayList<>(documentEntry.getTemplateMarking());
    markings.addAll(documentEntry.getCodeMarking());
    markings.addAll(documentEntry.getMisspelledMarking());
    markings.addAll(documentEntry.getSpoofMarking());

    Map<String, Object> properties = Map.of(
      "id", documentEntry.getId(),
      "filename", FilenameUtils.getBaseName(fileData.getPersistentPath()),
      "metadata", fileData.getMetadata(),
      "filepath", fileData.getPersistentPath(),
      "submitter", fileData.getSubmitter(),
      "indexed", documentEntry.isIndexed(),
      "pairIds", pairIds,
      "maxSimilarity", maxSimilarity,
      "markings", markings
    );

    return toJsonString(properties);
  }

  private static String toJsonString(Map<String, Object> propertiesMap) {
    try {
      return new ObjectMapper().writeValueAsString(propertiesMap);
    } catch (JsonProcessingException e) {
      log.warn("Failed to create JSON mapping: {}", e.getMessage());
      return "";
    }
  }
}
