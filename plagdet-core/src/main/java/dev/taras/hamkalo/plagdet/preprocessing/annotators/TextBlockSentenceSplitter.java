package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Goal of this class, is to create meaningful "sentences" from list of previously tokenized
 * lines. Problem it should solve, is that a lot of plag cases using sentence merging or
 * expansion.
 */
@Slf4j
public class TextBlockSentenceSplitter implements Annotator {

  private static final int MAX_SENT_LENGTH_WORD = 12; // counting for small words which will be lately removed

  private static final int MIN_SENT_LENGTH_WORD = 6;

  private final WordToSentenceProcessor<CoreLabel> wordToSentenceProcessor;

  public TextBlockSentenceSplitter() {
    this.wordToSentenceProcessor = new WordToSentenceProcessor<>();
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Sentence length splitter annotator started");
    List<CoreMap> newSentences = new ArrayList<>();
    var sentences = AnnotationUtils.sentences(annotation);
    int sentencesSize = sentences.size();
    for (int i = 0; i < sentencesSize; ) {
      CoreMap blockBegin = sentences.get(i);
      if (i == sentencesSize - 1) {
        newSentences.add(blockBegin);
        break;
      }

      int textBlockId = AnnotationUtils.textBlockId(blockBegin);
      var blockTokens = new ArrayList<CoreLabel>(AnnotationUtils.tokens(blockBegin));
      int j = i + 1;
      for (; j < sentencesSize; j++) {
        CoreMap next = sentences.get(j);
        if (AnnotationUtils.textBlockId(next) == textBlockId) {
          // TODO: here should be filtering
          blockTokens.addAll(AnnotationUtils.tokens(next));
        }
      }

      i = j;

      List<List<CoreLabel>> blockSentencesTokens = wordToSentenceProcessor.process(blockTokens);
      for (var blockSentenceTokens : blockSentencesTokens) {
        var blockSentence = AnnotationUtils.createSentence(blockSentenceTokens);
        AnnotationUtils.copy(PlagdetAnnotations.TextBlockId.class, blockSentence, blockBegin);
        newSentences.add(blockSentence);
      }
    }
    annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  public @NotNull Set<Integer> validateSentences() {
    return Collections.emptySet();
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of();
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class,
      PlagdetAnnotations.TextBlockId.class
    );
  }
}
