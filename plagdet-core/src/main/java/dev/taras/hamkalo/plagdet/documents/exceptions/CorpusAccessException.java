package dev.taras.hamkalo.plagdet.documents.exceptions;

public class CorpusAccessException extends RuntimeException {

  public CorpusAccessException(Throwable cause) {
    super(cause);
  }

  public CorpusAccessException(String message) {
    super(message);
  }

}
