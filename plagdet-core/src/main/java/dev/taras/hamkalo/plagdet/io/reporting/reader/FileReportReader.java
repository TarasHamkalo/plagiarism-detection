package dev.taras.hamkalo.plagdet.io.reporting.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.Report;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.io.reporting.writer.FileReportWriter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileReportReader {

  private final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  Map<Path, Submission> submissionMap = new HashMap<>();

  Map<Integer, DocumentEntry> documentEntryMap = new HashMap<>();

  Map<Integer, DocumentEntry> retrievedDocumentCache = new HashMap<>();

  Map<String, DocumentPair> documentPairsMap = new HashMap<>();

  public Report readReport(@NotNull Path source) {
    try {
      var paths = collectPathsFrom(source);
      var report = readOverview(paths);
      readSubmissions(source, paths);
      readIndexDocuments(source, paths);
      readPairs(source, paths);
      documentEntryMap.values().forEach(d -> {
        var documentPairs = d.getDocumentPairIds().stream()
          .map(documentPairsMap::get)
          .toList();
        d.setDocumentPairs(new PriorityQueue<>(documentPairs));
      });

      report.getSubmissions().addAll(submissionMap.values());
      report.getPairs().addAll(documentPairsMap.values());
      report.getDocumentsRetrievedFromIndex().addAll(retrievedDocumentCache.values());

      return report;
    } catch (IOException e) {
      throw new PlagdetException("Error reading report from path", e);
    }
  }

  private @NotNull Report readOverview(@NotNull Set<Path> paths) throws IOException {
    try {
      var overviewPath = paths.stream()
        .filter(p -> p.toString().contains(FileReportWriter.OVERVIEW_NAME))
        .findFirst()
        .get();

      return objectMapper.readValue(
        Files.readString(overviewPath), Report.class
      );
    } catch (NoSuchElementException e) {
      throw new PlagdetException("Could not find overivew file", e);
    }
  }

  private void readIndexDocuments(@NotNull Path source, Set<Path> paths) throws IOException {
    List<Path> indexedDocumentMetaPaths = paths.stream()
      .filter(path -> {
        Path relativePath = source.relativize(path);
        return relativePath.startsWith(FileReportWriter.INDEXED_FILES_DIR) &&
               relativePath.toString().endsWith("_meta.json");
      })
      .toList();

    for (var path : indexedDocumentMetaPaths) {
      var documentEntry = readDocumentEntry(path);
      retrievedDocumentCache.put(documentEntry.getId(), documentEntry);
    }
  }

  private Set<Path> collectPathsFrom(@NotNull Path source) throws IOException {
    try (var paths = Files.walk(source)) {
      return paths.collect(Collectors.toSet());
    }
  }

  private void readPairs(@NotNull Path source, @NotNull Set<Path> paths) throws IOException {
    List<Path> pairFiles = paths.stream()
      .filter(path -> {
        Path relativePath = source.relativize(path);
        return relativePath.startsWith(FileReportWriter.PAIRS_DIR) &&
               relativePath.toString().endsWith(".json");
      })
      .toList();

    for (Path pairFile : pairFiles) {
      var documentPair = objectMapper.readValue(
        Files.readString(pairFile), DocumentPair.class
      );

      documentPair.setFirst(getDocumentEntryById(documentPair.getFirstDocumentId()));
      documentPair.setSecond(getDocumentEntryById(documentPair.getSecondDocumentId()));
      documentPairsMap.put(documentPair.getId(), documentPair);
    }

  }

  private @NotNull DocumentEntry getDocumentEntryById(int id) {
    var documentEntry = documentEntryMap.get(id);
    if (documentEntry != null) {
      return documentEntry;
    }

    return retrievedDocumentCache.get(id);
  }

  private void readSubmissions(@NotNull Path source, @NotNull Set<Path> paths) throws IOException {
    submissionMap.clear();
    documentEntryMap.clear();

    List<Path> metaFiles = paths.stream()
      .filter(path -> {
        Path relativePath = source.relativize(path);
        return !relativePath.startsWith(FileReportWriter.INDEXED_FILES_DIR) &&
               relativePath.startsWith(FileReportWriter.FILES_DIR) &&
               relativePath.toString().endsWith("_meta.json");
      })
      .toList();

    for (Path metaFile : metaFiles) {
      DocumentEntry documentEntry = readDocumentEntry(metaFile);

      var parentPath = Path.of(documentEntry.getFileData().getPersistentPath()).getParent();
      submissionMap.computeIfAbsent(parentPath, (k) -> new Submission(k, new ArrayList<>()))
        .getDocuments()
        .add(documentEntry);

      documentEntryMap.put(documentEntry.getId(), documentEntry);
    }

  }

  private @NotNull DocumentEntry readDocumentEntry(@NotNull Path file) throws IOException {
    DocumentEntry documentEntry = objectMapper.readValue(
      Files.readString(file), DocumentEntry.class
    );

    Path parentDir = file.getParent();
    Path contentPath = parentDir.resolve(documentEntry.getId() + "_content.txt");

    if (Files.exists(contentPath)) {
      documentEntry.getFileData().setContent(Files.readString(contentPath));
    }
    return documentEntry;
  }
}
