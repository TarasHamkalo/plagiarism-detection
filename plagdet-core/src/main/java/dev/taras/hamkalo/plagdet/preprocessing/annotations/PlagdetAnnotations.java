package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  // TODO: used to communicate between stages (spell -> pos)
  public static class CorrectedSentencesAnnotation implements CoreAnnotation<List<CoreMap>> {

    @Override
    public Class<List<CoreMap>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  // TODO: used to communicate between stages (pos -> spell)
  public static class GuessedSentencesAnnotation implements CoreAnnotation<List<CoreMap>> {

    @Override
    public Class<List<CoreMap>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

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

  // TODO: remove
  public static class CorrectedWordAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
      return String.class;
    }

  }

  public static class GuessedLemmaAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }
}
