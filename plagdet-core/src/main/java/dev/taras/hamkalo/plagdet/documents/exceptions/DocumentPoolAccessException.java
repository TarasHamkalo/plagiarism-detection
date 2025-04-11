package dev.taras.hamkalo.plagdet.documents.exceptions;

public class DocumentPoolAccessException extends RuntimeException {

  public DocumentPoolAccessException(String message) {
    super(message);
  }

  public DocumentPoolAccessException(Throwable cause) {
    super(cause);
  }

}
