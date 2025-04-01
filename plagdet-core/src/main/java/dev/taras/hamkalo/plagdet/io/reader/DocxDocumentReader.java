package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class DocxDocumentReader implements DocumentReader {

  @Override
  public FileData getFileData(Path path) throws DocumentReadException {
    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
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

  public @NotNull String getContent(
    @NotNull Path path,
    @NotNull XWPFDocument doc
  ) throws DocumentReadException {
    try {
      ZipSecureFile.setMinInflateRatio(0);
      XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
      return xwpfWordExtractor.getText();
    } catch (Exception e) {
      throw new DocumentReadException(path, e);
    }
  }

  public @NotNull Metadata getMetadata(@NotNull Path path, @NotNull XWPFDocument doc) {
    try {
      var properties = doc.getProperties();
      var coreProps = properties.getCoreProperties();
      var extProps = properties.getExtendedProperties();

      String author = Optional.ofNullable(coreProps.getCreator())
        .orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(coreProps.getLastModifiedByUser())
        .orElse(Metadata.NONE.getModifier());
      var creationDate = coreProps.getCreated();
      var modificationDate = coreProps.getModified();
      long totalEditTime = extProps.getTotalTime() >= 0 ? extProps.getTotalTime() : 0L;
      var lastPrinted = coreProps.getLastPrinted();
      String revisionsNumber = Optional.ofNullable(coreProps.getRevision())
        .orElse("0");

      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(creationDate)
        .modificationDate(modificationDate)
        .totalEditTime(totalEditTime)
        .lastPrinted(lastPrinted)
        .revisionsNumber(Integer.parseInt(revisionsNumber))
        .build();
    } catch (Exception e) {
      log.error("Failed to read metadata for path {}", path);
      return Metadata.NONE;
    }

  }

  @Override
  public boolean supports(String extension) {
    return "DOCX".equalsIgnoreCase(extension);
  }

}
