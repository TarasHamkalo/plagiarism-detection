package dev.taras.hamkalo.plagdet.io.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taras.hamkalo.plagdet.comparision.model.Comparison;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.markings.MarkingUtils;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.io.report.model.DocumentEntryJsonModel;
import dev.taras.hamkalo.plagdet.io.report.model.OverviewJsonModel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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

  public void writeComparisons(
    @NotNull List<Comparison> comparisons,
    @NotNull Path destination
  ) throws IOException {
    Path pairsDir = IoUtils.resolveDirectory(destination, PAIRS_DIR, removeFilesOnExit);
    int comparisonsSize = comparisons.size();
    for (int i = 0; i < comparisonsSize; i++) {
      var comparison = comparisons.get(i);
      Path path = IoUtils.resolveFile(pairsDir, comparison.getId() + ".json", removeFilesOnExit);
      writeObject(path, comparison);
      log.trace("Wrote comparison to: {}", path);
    }
  }

  public void writeIndexedDocuments(
    @NotNull List<DocumentEntry> documentEntries,
    @NotNull Path destination
  ) throws IOException {
    Path indexedPath = IoUtils.resolveDirectory(destination, INDEXED_FILES_DIR, removeFilesOnExit);
    for (var documentEntry : documentEntries) {
      writeDocument(documentEntry, Collections.emptyList(), indexedPath);
    }
  }

  public void writeDocument(
    @NotNull DocumentEntry documentEntry,
    @NotNull List<Comparison> comparisons,
    @NotNull Path destination
  ) throws IOException {
    writeDocumentContent(documentEntry, destination);
    writeDocumentMetadata(documentEntry, comparisons, destination);
    log.trace("Wrote document entry {}", documentEntry.getFileData().getPersistentPath());
  }

  public void writeDocumentContent(
    @NotNull DocumentEntry documentEntry,
    @NotNull Path destination
  ) throws IOException {
    Path contentFilePath = IoUtils.resolveFile(
      destination, documentEntry.getId() + "_content.txt", removeFilesOnExit
    );

    Files.writeString(contentFilePath, documentEntry.getFileData().getContent());
  }

  public void writeDocumentMetadata(
    @NotNull DocumentEntry documentEntry,
    @NotNull List<Comparison> comparisons,
    @NotNull Path destination
  ) {
    Path metadataFilePath = IoUtils.resolveFile(
      destination, documentEntry.getId() + "_meta.json", removeFilesOnExit
    );

    var documentEntryModel = DocumentEntryJsonModel.builder()
      .id(documentEntry.getId())
      // TODO: remove after tested
      .isIndexed(documentEntry.getFileData().isIndexed())
      .fileData(documentEntry.getFileData())
      .pairIds(comparisons.stream().map(Comparison::getId).toList())
      .markings(MarkingUtils.getMarkings(documentEntry.getAnnotation()))
      .build();

    writeObject(metadataFilePath, documentEntryModel);
  }

  private <T> void writeObject(@NotNull Path destination, @NotNull T object) {
    try {
      mapper.writerFor(object.getClass()).writeValue(destination.toFile(), object);
    } catch (IOException e) {
      log.warn("Failed to create JSON mapping: {}", e.getMessage());
    }
  }

  public void writeOverview(
    @NotNull OverviewJsonModel overviewJsonModel,
    @NotNull Path destination
  ) {
    Path overviewPath = IoUtils.resolveFile(destination, OVERVIEW_NAME, removeFilesOnExit);
    log.trace("Wrote overview to {}", overviewPath);
    writeObject(overviewPath, overviewJsonModel);
  }
}
