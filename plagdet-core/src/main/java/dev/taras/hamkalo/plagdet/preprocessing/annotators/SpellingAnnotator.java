package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.spelling.SpellingCheckRule;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpellingAnnotator implements Annotator {

  private static final ThreadLocal<JLanguageTool> languageToolThreadLocal =
    ThreadLocal.withInitial(() -> {
      JLanguageTool tool = new JLanguageTool(Languages.getLanguageForShortCode("sk"));
      tool.getAllActiveRules().removeIf(rule -> !(rule instanceof SpellingCheckRule));
      return tool;
    });

  private static final Map<String, String> correctionCache = new ConcurrentHashMap<>();

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Spell annotator started");
    AnnotationUtils.sentences(annotation).parallelStream().forEach(this::processSentence);
  }

  private void processSentence(CoreMap sentence) {
    if (AnnotationUtils.tokens(sentence).stream().noneMatch(AnnotationUtils.isGuessed())) {
      return;
    }
    markSpellingWithLanguageTool(sentence);
  }

  @SneakyThrows
  private void markSpellingWithLanguageTool(CoreMap sentence) {
    var languageTool = languageToolThreadLocal.get();
    for (var token : AnnotationUtils.tokens(sentence)) {
      if (!AnnotationUtils.isGuessed().test(token)) {
        continue;
      }

      String word = token.word();
      if (correctionCache.containsKey(word)) {
        applyCorrection(token, correctionCache.get(word));
        continue;
      }

      var matches = languageTool.check(word, false, JLanguageTool.ParagraphHandling.NORMAL);
      for (var match : matches) {
        token.set(PlagdetAnnotations.MisspelledAnnotation.class, true);
        var best = pickBest(token, match.getSuggestedReplacements());
        if (best != null) {
          correctionCache.put(word, best);
          applyCorrection(token, best);
        }
      }
    }
  }

  private void applyCorrection(CoreLabel token, String best) {
    log.info("Correcting '{}' -> '{}'", token.word(), best);
    token.setWord(best); // TODO: verify that position info is not removed
    token.setLemma(null);
  }

  private String pickBest(CoreLabel misspelledToken, List<String> suggestions) {
    String misspelledTokenLetter = misspelledToken.word().toLowerCase().substring(0, 1);
    return suggestions.stream()
      .filter(s -> !s.contains(" ") && s.toLowerCase().startsWith(misspelledTokenLetter))
      .min(Comparator.comparingDouble(s -> distance(misspelledToken, s)))
      .orElse(null);
  }

  private double distance(CoreLabel token, String suggestion) {
    var levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    var doubleMetaphone = new DoubleMetaphone();

    String strippedWord = StringUtils.stripAccents(token.word());
    String strippedSuggestion = StringUtils.stripAccents(suggestion);
    var d1 = doubleMetaphone.doubleMetaphone(strippedWord);
    var d2 = doubleMetaphone.doubleMetaphone(strippedSuggestion);

    int noDiaDistance = levenshteinDistance.apply(strippedWord, strippedSuggestion);
    var metaphoneDistance = levenshteinDistance.apply(d1, d2);
    return metaphoneDistance + (1.5 * noDiaDistance);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.MisspelledAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class,
      CoreAnnotations.LemmaAnnotation.class,
      PlagdetAnnotations.GuessedLemmaAnnotation.class,
      PlagdetAnnotations.CodeSegmentAnnotation.class
    );
  }
}
