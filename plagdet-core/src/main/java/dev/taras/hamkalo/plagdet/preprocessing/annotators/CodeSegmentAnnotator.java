package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CodeSegmentAnnotator implements Annotator {

  private static final double GUESSED_RATIO_THRESHOLD = 0.8;

  public static final Pattern CODE_PATTERN = Pattern.compile(
    "(?sm)^(\\s*[\\w\\s]*([()])?.*\\{.*|.*}|.*;.*|#include\\s+[<\"].*[>\"])$"
  );

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Code segment annotator started");
    var sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    List<Integer> codeSentencesIndexes = new ArrayList<>();
    for (int i = 0; i < sentences.size(); i++) {
      var sentence = sentences.get(i);
      Matcher matcher = CODE_PATTERN.matcher(sentence.get(CoreAnnotations.TextAnnotation.class));
      var isCode = matcher.matches();
      var isLemmatizedCorrectly = isSentenceLemmatizedCorrectly(sentence);
      if (isCode && !isLemmatizedCorrectly) {
        codeSentencesIndexes.add(i);
      }

      mark(sentence, isCode);
    }
    annotation.set(PlagdetAnnotations.CodeSentencesAnnotation.class, codeSentencesIndexes);
  }

  private boolean isSentenceLemmatizedCorrectly(@NotNull CoreMap sentence) {
    var words = sentence.get(PlagdetAnnotations.WordsAnnotation.class);
    var guessedLemmaCount = words.stream().filter(isGuessed()).count();
    return (1 - ((double) guessedLemmaCount / words.size())) >= GUESSED_RATIO_THRESHOLD;
  }

  public Predicate<CoreLabel> isGuessed() {
    return t -> {
      var guessedLemmaAnnotation = t.get(PlagdetAnnotations.GuessedLemmaAnnotation.class);
      return guessedLemmaAnnotation != null && guessedLemmaAnnotation;
    };
  }

  private void mark(@NotNull CoreMap sentence, boolean isCode) {
    sentence.set(PlagdetAnnotations.CodeSegmentAnnotation.class, isCode);
    for (var token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
      token.set(PlagdetAnnotations.CodeSegmentAnnotation.class, isCode);
    }
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.CodeSegmentAnnotation.class,
      PlagdetAnnotations.CodeSentencesAnnotation.class,
      PlagdetAnnotations.GuessedLemmaAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
