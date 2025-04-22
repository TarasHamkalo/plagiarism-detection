package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import com.ibm.icu.text.SpoofChecker;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Pattern;

import static dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils.MIN_WORD_PER_SENTENCE;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WordFilteringAnnotator implements Annotator {

  private static final Pattern VALID_WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

  public static final int MIN_WORD_LENGTH = 3;

  @NotNull SpoofChecker spoofChecker;

  public WordFilteringAnnotator() {
    this.spoofChecker = new SpoofChecker.Builder()
      .setRestrictionLevel(SpoofChecker.RestrictionLevel.MODERATELY_RESTRICTIVE)
      .setChecks(SpoofChecker.ALL_CHECKS)
      .build();
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Word annotator started");
    AnnotationUtils.sentences(annotation).forEach(sentence -> {
      sentence.get(CoreAnnotations.TokensAnnotation.class)
        .removeIf(token -> {
          transformSpoofed(token); // icu4j -> replace with something simpler
          var word = StringUtils.stripAccents(token.word());
          return word.length() < MIN_WORD_LENGTH || !VALID_WORD_PATTERN.matcher(word).matches();
        });
    });

    AnnotationUtils.sentences(annotation)
      .removeIf(s -> AnnotationUtils.tokens(s).size() < MIN_WORD_PER_SENTENCE);

    annotation.set(CoreAnnotations.TokensAnnotation.class,
      AnnotationUtils.sentences(annotation).stream()
        .flatMap(sentence -> AnnotationUtils.tokens(sentence).stream())
        .toList()
    );
  }

  private void transformSpoofed(@NotNull CoreLabel token) {
    var isSpoof = spoofChecker.failsChecks(token.word());
    if (isSpoof) {
      token.set(PlagdetAnnotations.SpoofedWordAnnotation.class, true);
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