package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CodeSegmentAnnotator implements Annotator {

  public static final Pattern CODE_PATTERN = Pattern.compile(
    "(?sm)^(\\s*[\\w\\s]*([()])?.*\\{.*|.*}|.*;.*|#include\\s+[<\"].*[>\"])$"
  );

  @Override
  public void annotate(Annotation annotation) {
    for (var sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      Matcher matcher = CODE_PATTERN.matcher(sentence.get(CoreAnnotations.TextAnnotation.class));
      mark(sentence, matcher.matches());
    }
  }

  private void mark(CoreMap sentence, boolean isCode) {
    sentence.set(PlagdetAnnotations.CodeSegmentAnnotation.class, isCode);
    for (var token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
      token.set(PlagdetAnnotations.CodeSegmentAnnotation.class, isCode);
    }
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.CodeSegmentAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
