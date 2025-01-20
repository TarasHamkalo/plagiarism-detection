package dev.taras.hamkalo.plagdet.io.exception;

public class ParsingException extends RuntimeException {

  public ParsingException(String message) {
    super(message);
  }

  public ParsingException(Throwable cause) {
    super(cause);
  }

}
