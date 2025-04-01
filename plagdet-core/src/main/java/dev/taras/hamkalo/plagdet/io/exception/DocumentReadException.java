package dev.taras.hamkalo.plagdet.io.exception;

import java.nio.file.Path;

public class DocumentReadException extends RuntimeException {

  public DocumentReadException(String message) {
    super(message);
  }

  public DocumentReadException(Path path, Exception cause) {
    super(
      String.format("Failed to read document for path [%s], cause is [%s]", path, cause.getMessage())
    );
  }
}
