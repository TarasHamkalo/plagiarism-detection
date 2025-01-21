package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Set;

@Slf4j
public class WordAnnotator implements Annotator {

  @Override
  public void annotate(Annotation annotation) {
    annotation.set(PlagdetAnnotations.WordsAnnotation.class, new ArrayList<>());

    for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var words = new ArrayList<CoreLabel>();
      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

        var normalized = StringUtils.stripAccents(token.word());
        if (normalized.matches("[a-zA-Z]+")) {
          token.set(PlagdetAnnotations.WordAnnotation.class, token.word());
          token.set(PlagdetAnnotations.NoDiaAnnotation.class, normalized);

          words.add(token);
          annotation.get(PlagdetAnnotations.WordsAnnotation.class).add(token);
        } else {
          token.set(PlagdetAnnotations.WordAnnotation.class, null);
          token.set(PlagdetAnnotations.NoDiaAnnotation.class, token.word());
        }
      }

      sentence.set(PlagdetAnnotations.WordsAnnotation.class, words);
    }
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.WordAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.NoDiaAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class
    );
  }

}