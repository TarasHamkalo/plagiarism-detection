package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  public static class CodeSentencesAnnotation implements CoreAnnotation<List<int[]>> {

    @Override
    public Class<List<int[]>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  public static class SpoofedWordAnnotation implements CoreAnnotation<Boolean> {

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

  public static class TemplateSentenceAnnotation implements CoreAnnotation<Boolean> {

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
}
