package dev.taras.hamkalo.plagdet.indexing.exception;

public class IndexingException extends RuntimeException {

  public IndexingException(String message, Throwable cause) {
    super(message, cause);
  }

  public IndexingException(String message) {
    super(message);
  }

}
