
package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.comparision.ScoreUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.Iterator;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StanfordAdapterTokenizer extends Tokenizer {

  CoreDocument coreDocument;

  boolean filterStopwords;

  @NonFinal
  Iterator<CoreLabel> tokensIterator;

  CharTermAttribute termAttr;

  OffsetAttribute offsetAttr;

  public StanfordAdapterTokenizer(CoreDocument coreDocument, boolean filterStopwords) {
    this.coreDocument = coreDocument;
    this.tokensIterator = createFilteredWordIterator();

    this.termAttr = addAttribute(CharTermAttribute.class);
    this.offsetAttr = addAttribute(OffsetAttribute.class);
    this.filterStopwords = filterStopwords;
  }

  @Override
  public boolean incrementToken() {
    if (!tokensIterator.hasNext()) {
      return false;
    }

    var token = tokensIterator.next();
    termAttr.setEmpty().append(token.lemma());
    offsetAttr.setOffset(token.beginPosition(), token.endPosition());
    return true;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    this.offsetAttr.setOffset(0, 0);
    this.tokensIterator = createFilteredWordIterator();
  }

  private Iterator<CoreLabel> createFilteredWordIterator() {
    return coreDocument.sentences().stream()
      .filter(AnnotationUtils::isInformativeSentence)
      .flatMap(s -> s.tokens().stream())
      .filter(t ->
        !(filterStopwords && t.get(PlagdetAnnotations.StopWordAnnotation.class)) &&
        !t.get(PlagdetAnnotations.CodeSegmentAnnotation.class)
      )
      .toList()
      .iterator();
  }
}
