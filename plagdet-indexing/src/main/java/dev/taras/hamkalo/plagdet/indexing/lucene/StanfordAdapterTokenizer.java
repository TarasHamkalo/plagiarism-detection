
package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.util.Iterator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StanfordAdapterTokenizer extends Tokenizer {

  Iterator<CoreLabel> tokensIterator;

  CharTermAttribute termAttr;

  OffsetAttribute offsetAttr;

  public StanfordAdapterTokenizer(CoreDocument coreDocument, boolean filterStopwords) {
    this.tokensIterator = coreDocument.tokens().iterator();
    this.termAttr = addAttribute(CharTermAttribute.class);
    this.offsetAttr = addAttribute(OffsetAttribute.class);
  }

  @Override
  public boolean incrementToken() {
    if (!tokensIterator.hasNext()) {
        return false;
    }

    addAttribute(PositionIncrementAttribute.class);

    var token = next();
    if (token == null) {
      return false;
    }

    termAttr.setEmpty().append(token.lemma());
    offsetAttr.setOffset(token.beginPosition(), token.endPosition());
    return true;
  }

  private CoreLabel next() {
    while (tokensIterator.hasNext()) {
      var token = tokensIterator.next();
      if (!token.get(PlagdetAnnotations.StopWordAnnotation.class)) {
        return token;
      }
    }

    return null;
  }

}
