package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  /**
   * Serves as identifier for sentences, to which text block they belong.
   * During comparison phase sentences should be merged only inside of same block
   */
  public static class TextBlockId implements CoreAnnotation<Integer> {

    @Override
    public Class<Integer> getType() {
      return Integer.class;
    }

  }

  /**
   * During preprocessing stores max text block id, for faster sentence marking.
   */
  public static class TextBlockCounter implements CoreAnnotation<Integer> {

    @Override
    public Class<Integer> getType() {
      return Integer.class;
    }

  }

  /**
   * During preprocessing stores indexes of sentences, which represent text boundary.
   * Always stores index of previous (before ignored text block) sentence.
   * e.g. after removing large code block, we would not want to merge similar sentences
   * which are on different sides of it.
   */
  public static class TextBlockBoundaries implements CoreAnnotation<List<Integer>> {

    @Override
    public Class<List<Integer>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  /**
   * Stores a list of character offsets for each code segment detected
   */
  public static class CodeSegmentsAnnotation implements CoreAnnotation<List<int[]>> {

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
