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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpellingAnnotator implements Annotator {


  JLanguageTool languageTool;

  public SpellingAnnotator() {
    this.languageTool = new JLanguageTool(Languages.getLanguageForShortCode("sk"));
    for (var rule : languageTool.getAllActiveRules()) {
      if (!rule.getCategory().getId().toString().equals("TYPOS")) {
        languageTool.disableRule(rule.getId());
      }
    }
  }

  @Override
  public void annotate(Annotation annotation) {
    var sentences = annotation.get(PlagdetAnnotations.GuessedSentencesAnnotation.class).stream()
      .filter(s -> {
        var isCodeSegmentAnnotation = s.get(PlagdetAnnotations.CodeSegmentAnnotation.class);
        return isCodeSegmentAnnotation == null || !isCodeSegmentAnnotation;
      })
      .toList();

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

    for (var match : matches) {
      var misspelledToken = offsetToTokenMap.get(match.getFromPos());
      if (misspelledToken.get(PlagdetAnnotations.GuessedLemmaAnnotation.class)) {
        var suggestions = match.getSuggestedReplacements();
        var best = pickBest(misspelledToken, suggestions);
        if (best != null) {
          log.debug("[Misspell at {}] [Pick {}] [Options {}]", misspelledToken.word(), best, suggestions);
        }
      } else {
        log.debug("[Misspell at {}] [Ignored because was not guessed]", misspelledToken.word());
      }
    }

  }

  private @Nullable String pickBest(
    @NotNull CoreLabel misspelledToken,
    @NotNull List<String> suggestions
  ) {
    var noMultiWordSuggestions = suggestions.stream().filter(s -> !s.contains(" ")).toList();
    var misspelledTokenLetter = misspelledToken.get(PlagdetAnnotations.WordAnnotation.class).substring(0, 1);
    var startWithSameLetter = noMultiWordSuggestions.stream()
      .filter(s -> s.toLowerCase().startsWith(misspelledTokenLetter))
      .toList();

    if (startWithSameLetter.isEmpty()) {
      return null;
    }

    int min = 0;
    double minDistance = Double.MAX_VALUE;
    for (int i = 0; i < startWithSameLetter.size(); i++) {
      var suggestion = startWithSameLetter.get(i);
      double suggestionDistance = distance(misspelledToken, suggestion);
      if (suggestionDistance < minDistance) {
        min = i;
        minDistance = suggestionDistance;
      }
    }

    return startWithSameLetter.get(min);
  }

  private double distance(CoreLabel token, String suggestion) {
    String word = token.word();
    String strippedWord = StringUtils.stripAccents(word);
    String strippedSuggestion = StringUtils.stripAccents(suggestion);

    int noDiaDistance = LevenshteinDistance.getDefaultInstance().apply(strippedWord, strippedSuggestion);
    var doubleMetaphone = new DoubleMetaphone();
    var d1 = doubleMetaphone.doubleMetaphone(word);
    var d2 = doubleMetaphone.doubleMetaphone(suggestion);
    var metaphoneDistance = LevenshteinDistance.getDefaultInstance().apply(d1, d2);
    return metaphoneDistance + (1.5 * noDiaDistance);
  }

  private double jaccardCoefficient(String s1, String s2) {
    Set<String> set1 = new HashSet<>();
    Set<String> set2 = new HashSet<>();

    for (int i = 0; i < s1.length() - 1; i++) {
      set1.add(s1.substring(i, i + 2));
    }
    for (int i = 0; i < s2.length() - 1; i++) {
      set2.add(s2.substring(i, i + 2));
    }

    Set<String> intersection = new HashSet<>(set1);
    intersection.retainAll(set2);

    Set<String> union = new HashSet<>(set1);
    union.addAll(set2);

    return (double) intersection.size() / union.size();
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
      PlagdetAnnotations.SpoofAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.ValidatedSentenceAnnotation.class,
      PlagdetAnnotations.GuessedLemmaAnnotation.class,
      PlagdetAnnotations.CodeSegmentAnnotation.class
    );
  }

  private void attempt2() {
//    var documentText = annotation.get(CoreAnnotations.TextAnnotation.class);
//    var sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
//
//    var sentenceOffset = sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
//    var sentenceCodePointOffset = documentText.codePointCount(0, sentenceOffset);
//    if (sentenceOffset != sentenceCodePointOffset) {
//      log.debug("Not equal {} {}", sentenceOffset, sentenceCodePointOffset);
//    }
//    var nonWordTokens = sentence.get(CoreAnnotations.TokensAnnotation.class).stream()
//      .filter(t -> t.get(PlagdetAnnotations.WordAnnotation.class) == null)
//      .toList();
//
//    for (var t: nonWordTokens) {
//        System.out.println(t.word());
//        var tokenCodePointOffset = documentText.codePointCount(0, t.beginPosition());
//        if (tokenCodePointOffset != t.beginPosition()) {
//          log.debug("Not equal {} {}", t.beginPosition(), tokenCodePointOffset);
//        }
//        var tokenStart = tokenCodePointOffset - sentenceCodePointOffset;
//        textBuilder.replace(tokenStart, tokenStart + t.word().length() - 1, " ".repeat(t.word().length()));
//    }
//    log.debug("[Before {}] [After {}]", sentenceText, textBuilder.toString());
  }

  private void attempt1() {
//    String simpleText = sentence.get(CoreAnnotations.TextAnnotation.class);
////    String text = sentence.get(PlagdetAnnotations.WordsAnnotation.class).stream().map(CoreLabel::word).collect(Collectors.joining(""));
//    String text = sentence.get(CoreAnnotations.TokensAnnotation.class).stream()
//      .map(t -> {
//        if (t.get(PlagdetAnnotations.WordAnnotation.class) != null) {
//          return t.word();
//        }
//
//        return " ".repeat(t.word().length());
//      })
//      .collect(Collectors.joining(" "));
//
//    List<RuleMatch> matches = languageTool.check(text, false, JLanguageTool.ParagraphHandling.NORMAL);
//    if (matches.isEmpty()) {
//      return;
//    }
//
//    var sentenceOffset = sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
//    for (RuleMatch match : matches) {
//      var tokenTextSubstr = text.substring(match.getFromPos(), match.getToPos());
//
//      var optionalToken = sentence.get(CoreAnnotations.TokensAnnotation.class).stream()
//        .filter(t -> t.beginPosition() - sentenceOffset == match.getFromPos())
////        .filter(t -> isLocatedAt(annotation, sentence, t, match.getFromPos()))
//        .findFirst();
////      Martin Eliaš   Bianka Drábová   Samuel Ušala OS
//      var textFromToken = "not found";
//      if (optionalToken.isPresent()) {
//        textFromToken = optionalToken.get().word();
//      }
//
//      log.debug(
//        "[Token {}] [Text {}] [Options {}] [Message {}] [Rule {}]",
//        textFromToken,
//        tokenTextSubstr,
//        "",

    /// /        match.getSuggestedReplacements(),
//        match.getMessage(),
//        match.getRule().getId()
//      );
//    }
  }
}
