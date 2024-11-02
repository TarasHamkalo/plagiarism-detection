package dev.taras.hamkalo.plagdet.preprocessing.cleaner;

public class NopPreprocessor implements Preprocessor {

  @Override
  public String getName() {
    return "NOP";
  }

  @Override
  public String process(String text) {
    return text;
  }
}
