package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
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

@Slf4j
public class SentenceLengthSplitterAnnotator implements Annotator {

  private static final int MAX_SENT_LENGTH_WORD = 40;

  private static final int MIN_SENT_LENGTH_WORD = 20;

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Dynamic Sentence splitter annotator started");
    List<CoreMap> newSentences = new ArrayList<>();
    for (var sent : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var wordTokens = AnnotationUtils.wordTokens(sent);
      if (wordTokens.size() > MAX_SENT_LENGTH_WORD) {
        log.trace("Re-splitting sentence {} words long", wordTokens.size());

        List<CoreMap> splitSentences = splitLongSentence(wordTokens);
        log.trace("Created {} new sentences", splitSentences.size());

        newSentences.addAll(splitSentences);
      } else {
        newSentences.add(sent);
      }
    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  private @NotNull List<CoreMap> splitLongSentence(
    @NotNull List<CoreLabel> tokens
  ) {
    List<CoreMap> sentences = new ArrayList<>();
    int i = 0;
    for (; i < tokens.size() - MIN_SENT_LENGTH_WORD; i += MIN_SENT_LENGTH_WORD) {
      sentences.add(AnnotationUtils.createSentence(new ArrayList<>(tokens.subList(i, i + MIN_SENT_LENGTH_WORD))));
    }

    if (tokens.size() % MIN_SENT_LENGTH_WORD != 0) {
      sentences.add(AnnotationUtils.createSentence(new ArrayList<>(tokens.subList(i, tokens.size()))));
    }
    return sentences;
  }

//  public @NotNull CoreMap createSentence(@NotNull CoreMap sentence, @NotNull List<CoreLabel> tokens) {
//    CoreMap coreMap = new Annotation(sen));
//    coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
//    coreMap.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokens.get(0).beginPosition());
//    coreMap.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokens.get(tokens.size() - 1).endPosition());
////    return coreMap;
//  }

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
