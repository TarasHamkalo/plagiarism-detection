package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Set;

public class TextReader implements ContentReader, MetadataReader {

  @Override
  public String getContent(Path path) throws ContentReadException {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }

  }

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {
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
    } catch (IOException e) {
      throw new MetadataReadException("Unable to read metadata for file: " + path.toString());
    }
  }

  @Override
  public boolean supports(String extension) {
    return Set.of("txt", "tex", "").contains(extension.toLowerCase());
  }

}