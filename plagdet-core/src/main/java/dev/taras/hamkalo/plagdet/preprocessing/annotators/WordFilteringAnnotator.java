package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import com.ibm.icu.text.SpoofChecker;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WordFilteringAnnotator implements Annotator {

  private static final int MIN_WORD_PER_SENTENCE = 3;

  public static final int MIN_WORD_LENGTH = 3;

  SpoofChecker spoofChecker;

  public WordFilteringAnnotator() {
    this.spoofChecker = new SpoofChecker.Builder()
      .setRestrictionLevel(SpoofChecker.RestrictionLevel.MODERATELY_RESTRICTIVE)
      .setChecks(SpoofChecker.ALL_CHECKS)
      .build();
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Word annotator started");
//    log.trace("Tokens before " + AnnotationUtils.wordTokens(annotation).size());
    AnnotationUtils.sentences(annotation).forEach(sentence -> {
      sentence.get(CoreAnnotations.TokensAnnotation.class)
        .removeIf(token -> {
          transformSpoofed(token); // icu4j -> replace with something simpler
          if (token.word().length() < MIN_WORD_LENGTH) {
            return true;
          }

          return !StringUtils.stripAccents(token.word()).matches("[a-zA-Z]+");
        });
    });

    annotation.set(CoreAnnotations.TokensAnnotation.class,
      AnnotationUtils.sentences(annotation).stream()
        .flatMap(sentence -> AnnotationUtils.tokens(sentence).stream())
        .toList()
    );
//
    annotation.set(CoreAnnotations.SentencesAnnotation.class,
      mergeToMinLength(AnnotationUtils.sentences(annotation).stream().filter(s -> !AnnotationUtils.tokens(s).isEmpty()).toList())
    );
//    log.trace("Tokens after " + AnnotationUtils.wordTokens(annotation).size());
  }

  private @NotNull List<CoreMap> mergeToMinLength(@NotNull List<CoreMap> sentences) {
    var sentencesCopy = new ArrayList<>(sentences);
    int sentencesSize = sentencesCopy.size();
    for (int i = 0; i < sentencesSize - 1; i++) {
      var tokens = AnnotationUtils.tokens(sentencesCopy.get(i));
      if (tokens.size() < MIN_WORD_PER_SENTENCE) {
        var nextSentence = sentencesCopy.get(i + 1);
        AnnotationUtils.tokens(nextSentence).addAll(tokens);
        nextSentence.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, AnnotationUtils.begin(sentences.get(i)));
        sentencesCopy.remove(i);
        i--;
        sentencesSize--;
      }
    }

    return sentencesCopy;
  }

  private void transformSpoofed(@NotNull CoreLabel token) {
    var isSpoof = spoofChecker.failsChecks(token.word());
    token.set(PlagdetAnnotations.SpoofedWordAnnotation.class, isSpoof);
    if (isSpoof) {
      String skeleton = spoofChecker.getSkeleton(token.word());
      log.trace("Spoof detected at [{}] with skeleton of [{}]", token.word(), skeleton);

      token.setWord(skeleton);
    }
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.SpoofedWordAnnotation.class
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