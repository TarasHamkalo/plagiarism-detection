package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class DynamicSentenceSplitter implements Annotator {

  private static final int MAX_SENT_LENGTH_WORD = 24;

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Dynamic Sentence splitter annotator started");
    List<CoreMap> newSentences = new ArrayList<>();
    for (var sent : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      int wordTokensCount = countWordTokens(sent.get(CoreAnnotations.TokensAnnotation.class));
      if (wordTokensCount > MAX_SENT_LENGTH_WORD) {
        log.trace("Resplitting sentence {} words long", wordTokensCount);

        List<CoreMap> splitSentences = splitLongSentence(sent.get(CoreAnnotations.TokensAnnotation.class));
        log.trace("Created {} new sentences", splitSentences.size());

        newSentences.addAll(splitSentences);
      } else {
        newSentences.add(sent);
      }
    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  private List<CoreMap> splitLongSentence(List<CoreLabel> tokens) {
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
    return sentences;
  }

  private boolean isSentenceBoundary(CoreLabel token) {
    String afterWhitespace = token.get(CoreAnnotations.AfterAnnotation.class);
    if (afterWhitespace != null && afterWhitespace.contains("\n")) {
      return true;
    }

    return token.word().matches("[.!?;]");
  }

  private CoreMap createSentence(List<CoreLabel> tokens) {
    CoreMap coreMap = new Annotation(tokens.stream().map(CoreLabel::originalText).collect(Collectors.joining(" ")));
    coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
    coreMap.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokens.get(0).beginPosition());
    coreMap.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokens.get(tokens.size() - 1).endPosition());
    return coreMap;
  }

  // TODO: refactor pipeline
  private static int countWordTokens(List<CoreLabel> tokens) {
    int count = 0;
    for (var token : tokens) {
      var normalized = StringUtils.stripAccents(token.word());
      if (normalized.matches("[a-zA-Z]+")) {
        count++;
      }
    }
    return count;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.ValidatedSentenceAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class
    );
  }
}
