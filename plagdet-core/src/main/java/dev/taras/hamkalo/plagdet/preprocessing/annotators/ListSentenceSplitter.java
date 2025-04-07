package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class ListSentenceSplitter implements Annotator {

  private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\w+\\(\\)|\\w+)\\s*–");

  @Override
  public void annotate(Annotation annotation) {
    log.trace("List-aware Sentence Splitter started");

    List<CoreMap> newSentences = new ArrayList<>();
    for (var sent : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var wordTokens = AnnotationUtils.wordTokens(sent);
      String sentenceText = sent.get(CoreAnnotations.TextAnnotation.class);

      if (isListStructure(sentenceText)) {
        log.trace("Detected list structure, applying custom splitting");
        newSentences.addAll(splitListSentence(sentenceText));
      } else {
        newSentences.add(sent);
      }
    }

//      annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  private boolean isListStructure(String text) {
    // Check if text contains multiple list-like elements
    long matchCount = LIST_ITEM_PATTERN.matcher(text).results().count();
    return matchCount > 1; // If multiple matches, it's likely a list
  }

  private List<CoreMap> splitListSentence(String text) {
    System.out.println(text);
    return null;
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
