package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class DocDocumentReader implements DocumentReader {

  public static final int NANO_TIME_TO_MIN_PRECISION = 60 * 10_000_000;

  @Override
  public @NotNull FileData getFileData(@NotNull Path path) throws DocumentReadException {
    try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(path))) {
      Metadata metadata = getMetadata(path, doc);
      String content = getContent(path, doc);
      return FileData.builder()
        .content(content)
        .metadata(metadata)
        .build();
    } catch (Exception e) {
      throw new DocumentReadException(e.getMessage());
    }
  }

  public @Nullable String getContent(
    @NotNull Path path,
    @NotNull HWPFDocument doc
  ) throws DocumentReadException {
    try {
      WordExtractor extractor = new WordExtractor(doc);
      return extractor.getText();
    } catch (Exception e) {
      throw new DocumentReadException(path, e);
    }
  }

  public @NotNull Metadata getMetadata(@NotNull Path path, @NotNull HWPFDocument doc) {
    try {
      SummaryInformation summaryInfo = doc.getSummaryInformation();

      String author = Optional.ofNullable(summaryInfo.getAuthor())
        .orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(summaryInfo.getLastAuthor())
        .orElse(Metadata.NONE.getModifier());
      var creationDate = summaryInfo.getCreateDateTime();
      var modificationDate = summaryInfo.getLastSaveDateTime();
      var totalEditTime = summaryInfo.getEditTime() / NANO_TIME_TO_MIN_PRECISION;
      var lastPrinted = summaryInfo.getLastPrinted();
      var revisionsNumber = summaryInfo.getRevNumber();

      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(creationDate)
        .modificationDate(modificationDate)
        .totalEditTime(totalEditTime)
        .lastPrinted(lastPrinted)
        .revisionsNumber(revisionsNumber != null ? Integer.parseInt(revisionsNumber) : 0)
        .build();
    } catch (Exception e) {
      log.error("Failed to read metadata for path {}", path);
      return Metadata.NONE;
    }
  }

  @Override
  public boolean supports(String extension) {
    return "DOC".equalsIgnoreCase(extension);
  }
}
