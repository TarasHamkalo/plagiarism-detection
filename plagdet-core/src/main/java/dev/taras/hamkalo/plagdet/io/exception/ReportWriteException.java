package dev.taras.hamkalo.plagdet.io.exception;

public class ReportWriteException extends RuntimeException {

  public ReportWriteException(String message, Throwable cause) {
    super(message, cause);
  }

  public ReportWriteException(String message) {
    super(message);
  }

}
