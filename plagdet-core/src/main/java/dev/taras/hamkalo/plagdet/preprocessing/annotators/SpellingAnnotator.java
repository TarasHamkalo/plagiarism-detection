package dev.taras.hamkalo.plagdet.preprocessing.annotators;


import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.spelling.SpellingCheckRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpellingAnnotator implements Annotator {

  private static final ThreadLocal<JLanguageTool> languageToolThreadLocal =
    ThreadLocal.withInitial(() -> {
      JLanguageTool tool = new JLanguageTool(Languages.getLanguageForShortCode("sk"));
      for (var rule : tool.getAllActiveRules()) {
        if (!(rule instanceof SpellingCheckRule)) {
          tool.disableRule(rule.getId());
        }
      }
      return tool;
    });


  @Override
  public void annotate(Annotation annotation) {
    log.trace("Spell annotator started");
    for (var sentence : AnnotationUtils.sentences(annotation)) {
      var offsetToTokenMap = new HashMap<Integer, CoreLabel>();
      var hasGuessedLemma = false;
      var sentenceSb = new StringBuilder();
      for (var token : AnnotationUtils.wordTokens(sentence)) {
        hasGuessedLemma |= AnnotationUtils.isGuessed().test(token);
        offsetToTokenMap.put(sentenceSb.length(), token);
        sentenceSb.append(token.word()).append(" ");
      }
      if (hasGuessedLemma) {
        markSpellingWithLanguageTool(sentenceSb.toString(), offsetToTokenMap);
      }
    }
  }

  @SneakyThrows
  private void markSpellingWithLanguageTool(
    @NotNull String text,
    @NotNull Map<Integer, CoreLabel> offsetToTokenMap
  ) {
    var languageTool = languageToolThreadLocal.get();
    var matches = languageTool.check(text, false, JLanguageTool.ParagraphHandling.NORMAL);
    for (var match : matches) {
      var misspelledToken = offsetToTokenMap.get(match.getFromPos());
      if (misspelledToken != null && AnnotationUtils.isGuessed().test(misspelledToken)) { // ignore those tokens which were not properly tokenized with LanguageTool
        misspelledToken.set(PlagdetAnnotations.MisspelledAnnotation.class, true);
        var suggestions = match.getSuggestedReplacements();
        var best = pickBest(misspelledToken, suggestions);
        if (best == null) {
          continue;
        }

        misspelledToken.setWord(best);
        log.trace(
          "[Misspell at {}] [Pick {}] [Options {}]", misspelledToken.word(), best, suggestions
        );
      }
    }
  }

  private @Nullable String pickBest(
    @NotNull CoreLabel misspelledToken,
    @NotNull List<String> suggestions
  ) {
    String misspelledTokenLetter = misspelledToken.word().toLowerCase().substring(0, 1);

    String bestMatch = null;
    double minDistance = Double.MAX_VALUE;

    for (String suggestion : suggestions) {
      if (suggestion.contains(" ") || !suggestion.toLowerCase().startsWith(misspelledTokenLetter)) {
        continue;
      }

      double suggestionDistance = distance(misspelledToken, suggestion);
      if (suggestionDistance < minDistance) {
        minDistance = suggestionDistance;
        bestMatch = suggestion;
      }
    }

    return bestMatch;
  }

  private double distance(@NotNull CoreLabel token, @NotNull String suggestion) {
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
    return Set.of(
      PlagdetAnnotations.MisspelledAnnotation.class,
      PlagdetAnnotations.CorrectedWordAnnotation.class,
      PlagdetAnnotations.CorrectedSentencesAnnotation.class
    );
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

//    log.trace("W1 [{}] M1 [{}] -- W2 [{}] M2 [{}] -- S [{}]", strippedWord, d1, strippedSuggestion, d2, metaphoneDistance + (1.5 * noDiaDistance));
