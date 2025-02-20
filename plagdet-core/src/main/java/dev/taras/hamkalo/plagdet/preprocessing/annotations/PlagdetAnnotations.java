package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  public static class WordAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
      return String.class;
    }

  }

  public static class WordsAnnotation implements CoreAnnotation<List<CoreLabel>> {

    @Override
    public Class<List<CoreLabel>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  public static class SpoofAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class StopWordAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class NoDiaAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
      return String.class;
    }

  }

  public static class TemplateAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class CodeSegmentAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class CodeSentencesAnnotation implements CoreAnnotation<List<Integer>> {

    @Override
    public Class<List<Integer>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  public static class ValidatedSentenceAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class MisspelledAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class GuessedLemmaAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public static class GuessedSentencesAnnotation implements CoreAnnotation<List<CoreMap>> {

    @Override
    public Class<List<CoreMap>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }
}
