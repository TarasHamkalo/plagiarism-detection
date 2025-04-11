package dev.taras.hamkalo.plagdet.io.filedata.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Set;

@Slf4j
public class TextFileDataReader implements FileDataReader {

  @Override
  public @NotNull FileData getFileData(@NotNull Path path) throws DocumentReadException {
    Metadata metadata = getMetadata(path);
    String content = getContent(path);
    return FileData.builder()
      .content(content)
      .metadata(metadata)
      .build();
  }

  public @NotNull String getContent(@NotNull Path path) throws DocumentReadException {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new DocumentReadException(path, e);
    }
  }

  public @NotNull Metadata getMetadata(@NotNull Path path) {
    try {
      var attributes = Files.readAttributes(path, BasicFileAttributes.class);

      return Metadata.builder()
        .creator(null)
        .modifier(null)
        .creationDate(new Date(attributes.creationTime().toMillis()))
        .totalEditTime(0L)
        .revisionsNumber(0)
        .modificationDate(new Date(attributes.lastModifiedTime().toMillis()))
        .lastPrinted(null)
        .build();
    } catch (Exception e) {
      log.error("Failed to read metadata for path {}", path);
      return Metadata.NONE;
    }
  }

  @Override
  public boolean supports(String extension) {
    return Set.of("txt", "tex", "").contains(extension.toLowerCase());
  }

}