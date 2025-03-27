package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    List<CoreMap> filteredSentences = new ArrayList<>();
    List<int[]> codeSentenceOffsets = new ArrayList<>();

    for (var sentence : sentences) {
      boolean codePatternMatches =
        CODE_PATTERN.matcher(sentence.get(CoreAnnotations.TextAnnotation.class)).matches();
      boolean hasLemmatizationErrors = !isLemmatizedCorrectly(sentence);
      if (codePatternMatches && hasLemmatizationErrors) {
        codeSentenceOffsets.add(new int[]{
          sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
          sentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class)
        });
      } else {
        filteredSentences.add(sentence);
      }
    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, filteredSentences);
    annotation.set(CoreAnnotations.TokensAnnotation.class,
      AnnotationUtils.sentences(annotation).stream()
        .flatMap(sentence -> AnnotationUtils.wordTokens(sentence).stream())
        .toList()
    );
    annotation.set(PlagdetAnnotations.CodeSentencesAnnotation.class, codeSentenceOffsets);
  }

  private boolean isLemmatizedCorrectly(@NotNull CoreMap sentence) {
    var tokens = AnnotationUtils.wordTokens(sentence);
    long guessedLemmaCount = tokens.stream().filter(AnnotationUtils.isGuessed()).count();
    return (1 - ((double) guessedLemmaCount / tokens.size())) >= GUESSED_RATIO_THRESHOLD;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.CodeSentencesAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      PlagdetAnnotations.GuessedLemmaAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
