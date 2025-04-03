package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SentenceLengthSplitterAnnotator implements Annotator {

  private static final int MAX_SENT_LENGTH_WORD = 20;

  private static final int MIN_SENT_LENGTH_WORD = 3;

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Dynamic Sentence splitter annotator started");
    List<CoreMap> newSentences = new ArrayList<>();
    for (var sent : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      long wordTokensCount = countWordTokens(AnnotationUtils.wordTokens(sent));
      if (wordTokensCount > MAX_SENT_LENGTH_WORD) {
        log.trace("Re-splitting sentence {} words long", wordTokensCount);
        List<CoreMap> splitSentences = splitLongSentence(sent.get(CoreAnnotations.TokensAnnotation.class));
        log.trace("Created {} new sentences", splitSentences.size());

        newSentences.addAll(splitSentences);
      } else {
        newSentences.add(sent);
      }
    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  private @NotNull List<CoreMap> splitLongSentence(@NotNull List<CoreLabel> tokens) {
    List<CoreMap> sentences = new ArrayList<>();
    List<CoreLabel> currentSentenceTokens = new ArrayList<>();

    for (CoreLabel token : tokens) {
      currentSentenceTokens.add(token);
      if (isSentenceBoundary(token)) {
        CoreMap sentence = createSentence(currentSentenceTokens);
        sentences.add(sentence);
        currentSentenceTokens = new ArrayList<>();
      }
    }

    if (!currentSentenceTokens.isEmpty()) {
      CoreMap sentence = createSentence(currentSentenceTokens);
      sentences.add(sentence);
    }

    return mergeShortSentences(sentences);
  }

  private @NotNull List<CoreMap> mergeShortSentences(@NotNull List<CoreMap> sentences) {
    for (int i = sentences.size() - 1; i > 0; i--) {
      var sentence = sentences.get(i);
      if (countWordTokens(AnnotationUtils.wordTokens(sentence)) < MIN_SENT_LENGTH_WORD) {
        var previousSentence = sentences.get(i - 1);
        AnnotationUtils.wordTokens(previousSentence).addAll(AnnotationUtils.wordTokens(sentence));
        previousSentence.set(
          CoreAnnotations.CharacterOffsetEndAnnotation.class, AnnotationUtils.end(sentence)
        );
        sentences.remove(i);

      }
    }

    return sentences;
  }

  private boolean isSentenceBoundary(@NotNull CoreLabel token) {
    String afterWhitespace = token.get(CoreAnnotations.AfterAnnotation.class);
    if (afterWhitespace != null && afterWhitespace.contains("\n")) {
      return true;
    }

    return token.word().matches("[.!?;]");
  }

  private static long countWordTokens(@NotNull List<CoreLabel> tokens) {
    return tokens.parallelStream()
      .filter(t -> StringUtils.stripAccents(t.word()).matches("[a-zA-Z]+"))
      .count();
  }

  private @NotNull CoreMap createSentence(@NotNull List<CoreLabel> tokens) {
    CoreMap coreMap = new Annotation(AnnotationUtils.tokensText(tokens));
    coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
    coreMap.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokens.get(0).beginPosition());
    coreMap.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokens.get(tokens.size() - 1).endPosition());
    return coreMap;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of();
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
