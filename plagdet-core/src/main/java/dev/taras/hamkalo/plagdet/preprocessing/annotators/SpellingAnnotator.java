package dev.taras.hamkalo.plagdet.preprocessing.annotators;


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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.spelling.SpellingCheckRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpellingAnnotator implements Annotator {

  JLanguageTool languageTool;

  public SpellingAnnotator() {
    languageTool = prepareLanguageTool();
  }

  private @NotNull JLanguageTool prepareLanguageTool() {
    var languageTool = new JLanguageTool(Languages.getLanguageForShortCode("sk"));
    for (var rule : languageTool.getAllActiveRules()) {
      if (!(rule instanceof SpellingCheckRule)) {
        languageTool.disableRule(rule.getId());
      }
    }

    return languageTool;
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Spell annotator started");
    var sentences = annotation.get(PlagdetAnnotations.GuessedSentencesAnnotation.class).stream()
      .filter(s -> {
        var isCodeSegmentAnnotation = s.get(PlagdetAnnotations.CodeSegmentAnnotation.class);
        return isCodeSegmentAnnotation == null || !isCodeSegmentAnnotation;
      })
      .toList();
    annotation.set(PlagdetAnnotations.CorrectedSentencesAnnotation.class, new ArrayList<>());
    for (var sentence : sentences) {
      markSpellingWithLanguageTool(annotation, sentence);
    }
  }

  @SneakyThrows
  private void markSpellingWithLanguageTool(Annotation annotation, CoreMap sentence) {
    var offsetToTokenMap = new HashMap<Integer, CoreLabel>();
    StringBuilder textBuilder = new StringBuilder();
    for (var token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
      var word = token.get(PlagdetAnnotations.WordAnnotation.class);
      if (word != null) {
        offsetToTokenMap.put(textBuilder.length(), token);
        textBuilder.append(word).append(" ");
      }

    }
    var matches = languageTool.check(
      textBuilder.toString(), false, JLanguageTool.ParagraphHandling.NORMAL
    );

    if (matches.isEmpty()) {
      return;
    }
    var hasCorrections = false;
    for (var match : matches) {
      var misspelledToken = offsetToTokenMap.get(match.getFromPos());
      if (misspelledToken.get(PlagdetAnnotations.GuessedLemmaAnnotation.class)) {
        misspelledToken.set(PlagdetAnnotations.MisspelledAnnotation.class, true);
        var suggestions = match.getSuggestedReplacements();
        var best = pickBest(misspelledToken, suggestions);
        if (best != null) {
          hasCorrections = true;
          misspelledToken.set(PlagdetAnnotations.CorrectedWordAnnotation.class, best);
          log.trace(
            "[Misspell at {}] [Pick {}] [Options {}]", misspelledToken.word(), best, suggestions
          );

        }
      }
    }

    if (hasCorrections) {
      annotation.get(PlagdetAnnotations.CorrectedSentencesAnnotation.class).add(sentence);
    }

  }

  private @Nullable String pickBest(
    @NotNull CoreLabel misspelledToken,
    @NotNull List<String> suggestions
  ) {

    var misspelledTokenLetter = misspelledToken.get(PlagdetAnnotations.WordAnnotation.class)
      .toLowerCase()
      .substring(0, 1);

    var filtered = suggestions.stream()
      .filter(s -> !s.contains(" "))
      .filter(s -> s.toLowerCase().startsWith(misspelledTokenLetter))
      .toList();

    if (filtered.isEmpty()) {
      return null;
    }

    int min = 0;
    double minDistance = Double.MAX_VALUE;
    for (int i = 0; i < filtered.size(); i++) {
      var suggestion = filtered.get(i);
      double suggestionDistance = distance(misspelledToken, suggestion);
      if (suggestionDistance < minDistance) {
        min = i;
        minDistance = suggestionDistance;
      }
    }

    return filtered.get(min);
  }

  private double distance(CoreLabel token, String suggestion) {
    String word = token.get(PlagdetAnnotations.WordAnnotation.class);
    String strippedWord = StringUtils.stripAccents(word);
    String strippedSuggestion = StringUtils.stripAccents(suggestion);

    int noDiaDistance = LevenshteinDistance.getDefaultInstance().apply(strippedWord, strippedSuggestion);
    var doubleMetaphone = new DoubleMetaphone();
    var d1 = doubleMetaphone.doubleMetaphone(word);
    var d2 = doubleMetaphone.doubleMetaphone(suggestion);
    var metaphoneDistance = LevenshteinDistance.getDefaultInstance().apply(d1, d2);
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
      PlagdetAnnotations.SpoofedWordAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.GuessedLemmaAnnotation.class,
      PlagdetAnnotations.CodeSegmentAnnotation.class
    );
  }

}
