package dev.taras.hamkalo.plagdet.exception;

public class PlagdetException extends RuntimeException {

  public PlagdetException(String message) {
    super(message);
  }

  public PlagdetException(String message, Throwable cause) {
    super(message, cause);
  }

}
