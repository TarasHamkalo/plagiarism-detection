package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AnnotationUtils {

  public static final int MIN_WORD_PER_SENTENCE = 2;

  public static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{3,}");

  public static <V> void copy(
    @NotNull Class<? extends edu.stanford.nlp.util.TypesafeMap.Key<V>> clazz,
    @NotNull CoreMap dest,
    @NotNull CoreMap source
  ) {
    dest.set(clazz, source.get(clazz));
  }


  public static @NotNull List<List<CoreLabel>> mergeToMinLength(
    @NotNull List<List<CoreLabel>> sentences,
    int minLength
  ) {
    var sentencesCopy = new ArrayList<>(sentences);
    int sentencesSize = sentencesCopy.size();
    for (int i = 0; i < sentencesSize - 1; i++) {
      var wordTokens = AnnotationUtils.wordTokens(sentencesCopy.get(i));
      if (wordTokens.size() < minLength) {
        var nextSentence = sentencesCopy.get(i + 1);
        var newTokens = new ArrayList<>(sentencesCopy.get(i));
        newTokens.addAll(nextSentence);
        sentencesCopy.set(i + 1, newTokens);
        sentencesCopy.remove(i);
        sentencesSize--;
        i--;
      }
    }

    List<CoreLabel> lastSentenceTokens = sentencesCopy.get(sentencesSize - 1);
    if (AnnotationUtils.wordTokens(lastSentenceTokens).size() < minLength) {
      sentencesCopy.get(sentencesSize - 2).addAll(lastSentenceTokens);
      sentencesCopy.remove(sentencesSize - 1);
    }

    return sentencesCopy;
  }

  public static @NotNull Integer begin(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public static @NotNull Integer end(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
  }

  public static @NotNull Integer textBlockId(@NotNull CoreMap map) {
    var id = map.get(PlagdetAnnotations.TextBlockId.class);
    if (id == null) {
      return -1;
    }

    return id;
  }

  public static @NotNull String tokensText(@NotNull CoreMap coreMap) {
    return tokensText(AnnotationUtils.tokens(coreMap));
  }

  public static @NotNull String tokensText(@NotNull List<? extends CoreMap> coreMaps) {
    return coreMaps.stream()
      .map(c -> c.get(CoreAnnotations.TextAnnotation.class))
      .collect(Collectors.joining(" "));
  }

  public static @NotNull List<int[]> templateSentences(@NotNull CoreMap map) {
    var sentences = map.get(PlagdetAnnotations.TemplateSentencesAnnotation.class);
    return sentences == null ? Collections.emptyList() : sentences;
  }

  public static @NotNull List<int[]> codeSentences(@NotNull CoreMap map) {
    var sentences = map.get(PlagdetAnnotations.CodeSegmentsAnnotation.class);
    return sentences == null ? Collections.emptyList() : sentences;
  }

  public static @NotNull List<CoreMap> sentences(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.SentencesAnnotation.class);
  }

  public static @NotNull List<CoreLabel> wordTokens(@NotNull List<CoreLabel> tokens) {
    return tokens.stream()
      .filter(t -> WORD_PATTERN.matcher(StringUtils.stripAccents(t.word())).matches())
      .toList();
  }

  public static @NotNull List<CoreLabel> wordTokens(@NotNull CoreMap map) {
    return wordTokens(AnnotationUtils.tokens(map));
  }

  public static @NotNull List<CoreLabel> tokens(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.TokensAnnotation.class);
  }

  public static @NotNull String lemmasText(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.TokensAnnotation.class).stream()
      .map(CoreLabel::lemma)
      .collect(Collectors.joining(" "));
  }

  public static @NotNull List<String> lemmas(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.TokensAnnotation.class).stream()
      .map(CoreLabel::lemma)
      .toList();
  }

  public static @NotNull List<String> tags(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.TokensAnnotation.class).stream()
      .map(CoreLabel::tag)
      .toList();
  }

  public static @NotNull CoreMap createSentence(
    @NotNull List<CoreLabel> tokens,
    int begin,
    int end
  ) {
    CoreMap coreMap = new Annotation("");
    coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
    coreMap.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, begin);
    coreMap.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, end);
    return coreMap;
  }

  public static @NotNull CoreMap createSentence(@NotNull List<CoreLabel> tokens) {
    CoreMap coreMap = new Annotation(AnnotationUtils.tokensText(tokens));
    coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
    coreMap.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokens.get(0).beginPosition());
    coreMap.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokens.get(tokens.size() - 1).endPosition());
    return coreMap;
  }

  public static @NotNull Predicate<CoreLabel> isMisspelledToken() {
    return t -> Boolean.TRUE.equals(t.get(PlagdetAnnotations.MisspelledAnnotation.class));
  }

  public static @NotNull Predicate<CoreLabel> isSpoofedToken() {
    return t -> Boolean.TRUE.equals(t.get(PlagdetAnnotations.SpoofedWordAnnotation.class));
  }

  public static @NotNull Predicate<CoreLabel> isGuessed() {
    return t -> Boolean.TRUE.equals(t.get(PlagdetAnnotations.GuessedLemmaAnnotation.class));
  }

  public static @NotNull List<String> getCorrection(@NotNull CoreLabel token) {
    return List.of(token.word());
  }
}
