package dev.taras.hamkalo.plagdet.documents;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.nio.file.Path;

// TODO: make clean method to remove content strings after transformation
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentEntry {

  Long id;

  Path path;

  @NonFinal
  String content;

  @NonFinal
  String normalizedContent;

  public DocumentEntry(String path) {
    this.id = (long) path.hashCode();
    this.path = Path.of(path);
  }
}
