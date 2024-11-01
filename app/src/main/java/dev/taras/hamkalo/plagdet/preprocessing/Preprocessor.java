package dev.taras.hamkalo.plagdet.preprocessing;

public interface Preprocessor {

  String getName();

  String process(String text);

}
