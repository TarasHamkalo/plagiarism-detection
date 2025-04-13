package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  /**
   * Stores a list of character offsets for each code sentence detected
   */
  public static class CodeSentencesAnnotation implements CoreAnnotation<List<int[]>> {

    @Override
    public Class<List<int[]>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  /**
   * Stores a list of character offsets for each template sentence detected
   */
  public static class TemplateSentencesAnnotation implements CoreAnnotation<List<int[]>> {

    @Override
    public Class<List<int[]>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  /**
   * Added to each token being detected as Unicode spoof by ICU4j
   */
  public static class SpoofedWordAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  /**
   * Added to each misspelled token
   */
  public static class MisspelledAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  /**
   * Added to each token not known by morphological dictionary
   */
  public static class GuessedLemmaAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  /**
   * Used for dependency specification, not added to any annotation map
   */
  public static class StopWordAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  /**
   * Used for dependency specification, not added to any annotation map
   */
  public static class CodeSegmentAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }
}
