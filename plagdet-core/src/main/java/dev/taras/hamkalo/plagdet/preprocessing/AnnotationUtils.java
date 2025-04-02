package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.Plagdet;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AnnotationUtils {

  public static <V> void copy(
    @NotNull Class<? extends edu.stanford.nlp.util.TypesafeMap.Key<V>> clazz,
    @NotNull CoreMap dest,
    @NotNull CoreMap source
  ) {
    dest.set(clazz, source.get(clazz));
  }

  public static @NotNull Integer begin(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public static @NotNull Integer end(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
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
    var sentences = map.get(PlagdetAnnotations.CodeSentencesAnnotation.class);
    return sentences == null ? Collections.emptyList() : sentences;
  }

  public static @NotNull List<CoreMap> sentences(@NotNull CoreMap map) {
    return map.get(CoreAnnotations.SentencesAnnotation.class);
  }

  public static @NotNull List<CoreLabel> wordTokens(@NotNull CoreMap map) {
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

  public static boolean isInformativeSentence(@NotNull CoreSentence sentence) {
    return isInformative(sentence.coreMap());
  }

  public static boolean isInformative(@NotNull CoreMap coreMap) {
    var stopWordAnnotation = coreMap.get(PlagdetAnnotations.StopWordAnnotation.class);
    var codeSegmentAnnotation = coreMap.get(PlagdetAnnotations.CodeSegmentAnnotation.class);
    var templateAnnotation = coreMap.get(PlagdetAnnotations.TemplateSentenceAnnotation.class);

    boolean isStopWord = stopWordAnnotation != null && stopWordAnnotation;
    boolean isCodeSegment = codeSegmentAnnotation != null && codeSegmentAnnotation;
    boolean isTemplate = templateAnnotation != null && templateAnnotation;

    return !isCodeSegment && !isTemplate && !isStopWord;
  }

  public static void addTemplateAnnotation(
    @NotNull Annotation annotation,
    @NotNull CoreMap sentence,
    int sentenceIndex
  ) {
    var templateSentences = annotation.get(PlagdetAnnotations.TemplateSentencesAnnotation.class);
    if (templateSentences == null) {
      templateSentences = new ArrayList<>();
      annotation.set(PlagdetAnnotations.TemplateSentencesAnnotation.class, templateSentences);
    }

    templateSentences.add(new int[]{
      AnnotationUtils.begin(sentence),
      AnnotationUtils.end(sentence),
    });

    AnnotationUtils.sentences(annotation).remove(sentenceIndex);
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
    // TODO: proper correction suggestion
    return List.of(token.word());
//    var correctedWordAnnotation = token.get(PlagdetAnnotations.CorrectedWordAnnotation.class);
//    return correctedWordAnnotation != null ? List.of(correctedWordAnnotation) : Collections.emptyList();
  }
}
