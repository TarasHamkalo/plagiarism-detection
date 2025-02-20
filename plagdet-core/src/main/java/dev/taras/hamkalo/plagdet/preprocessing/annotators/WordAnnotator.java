package dev.taras.hamkalo.plagdet.preprocessing.annotators;

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
import com.ibm.icu.text.SpoofChecker;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WordAnnotator implements Annotator {

  public static final String ALLOWED_ACCENTS = "áäčďéíĺľňóôŕšťúýž";

  SpoofChecker spoofChecker;

  public WordAnnotator() {
    this.spoofChecker = new SpoofChecker.Builder()
      .setRestrictionLevel(SpoofChecker.RestrictionLevel.MODERATELY_RESTRICTIVE)
      .setChecks(SpoofChecker.ALL_CHECKS)
      .build();
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Word annotator started");
    annotation.set(PlagdetAnnotations.WordsAnnotation.class, new ArrayList<>());

    for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var words = new ArrayList<CoreLabel>();
      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        var word = getCleanWord(token);

        var noAccentsWord = StringUtils.stripAccents(word);
        if (noAccentsWord.matches("[a-zA-Z]+")) {
          token.set(PlagdetAnnotations.WordAnnotation.class, word);
          token.set(PlagdetAnnotations.NoDiaAnnotation.class, noAccentsWord);

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

  private @NotNull String getCleanWord(@NotNull CoreLabel token) {
    var word = token.word();

    var isSpoof = spoofChecker.failsChecks(word);
    token.set(PlagdetAnnotations.SpoofAnnotation.class, isSpoof);
    if (isSpoof) {
      var sb = new StringBuilder(spoofChecker.getSkeleton(word));
      var tokenChars = word.toCharArray();
      for (int i = 0; i < tokenChars.length; i++) {
        var ch = tokenChars[i];
        if (ALLOWED_ACCENTS.indexOf(ch) != -1) {
          sb.replace(i, i + 1, "" + ch);
        }
      }

      word = sb.toString();
      log.debug("Spoof detected at [{}] with skeleton of [{}]", token.word(), sb);
    }
    return word;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.WordAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.NoDiaAnnotation.class,
      PlagdetAnnotations.SpoofAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class,
      PlagdetAnnotations.ValidatedSentenceAnnotation.class
    );
  }

}