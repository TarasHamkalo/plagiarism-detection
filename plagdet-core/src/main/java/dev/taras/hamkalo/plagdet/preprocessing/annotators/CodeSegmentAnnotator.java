package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: increase accuracy by checking amount of no tag text elements
@Slf4j
public class CodeSegmentAnnotator implements Annotator {

  public static final Pattern CODE_PATTERN = Pattern.compile(
    "(?sm)^(\\s*[\\w\\s]*([()])?.*\\{.*|.*}|.*;.*|#include\\s+[<\"].*[>\"])$"
  );

  @Override
  public void annotate(Annotation annotation) {
    var sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    List<Integer> codeSentencesIndexes = new ArrayList<>();
    for (int i = 0; i < sentences.size(); i++) {
      var sentence = sentences.get(i);
      Matcher matcher = CODE_PATTERN.matcher(sentence.get(CoreAnnotations.TextAnnotation.class));
      var isCode = matcher.matches();
      if (isCode) {
        codeSentencesIndexes.add(i);
      }

      mark(sentence, isCode);
    }
    annotation.set(PlagdetAnnotations.CodeSentencesAnnotation.class, codeSentencesIndexes);
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
      PlagdetAnnotations.CodeSegmentAnnotation.class,
      PlagdetAnnotations.CodeSentencesAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
