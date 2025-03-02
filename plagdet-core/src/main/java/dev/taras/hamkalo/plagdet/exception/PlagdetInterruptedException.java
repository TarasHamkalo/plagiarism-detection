package dev.taras.hamkalo.plagdet.exception;

public class PlagdetInterruptedException extends PlagdetException {
  public PlagdetInterruptedException(String message) {
    super(message);
  }

  public PlagdetInterruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}
