package dev.taras.hamkalo.plagdet.preprocessing.annotations;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

public class PlagdetAnnotations {

  public static class WordAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
      return String.class;
    }

  }

  public class WordsAnnotation implements CoreAnnotation<List<CoreLabel>> {

    @Override
    public Class<List<CoreLabel>> getType() {
      return ErasureUtils.uncheckedCast(List.class);
    }

  }

  public class StopWordAnnotation implements CoreAnnotation<Boolean> {

    @Override
    public Class<Boolean> getType() {
      return Boolean.class;
    }

  }

  public class NoDiaAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
      return String.class;
    }

  }
}
