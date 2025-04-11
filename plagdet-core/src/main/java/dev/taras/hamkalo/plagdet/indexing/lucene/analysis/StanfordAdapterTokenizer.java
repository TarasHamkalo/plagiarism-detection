
package dev.taras.hamkalo.plagdet.indexing.lucene.analysis;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
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

  Annotation annotation;

  @NonFinal
  Iterator<CoreLabel> tokensIterator;

  CharTermAttribute termAttr;

  OffsetAttribute offsetAttr;

  public StanfordAdapterTokenizer(Annotation annotation) {
    this.annotation = annotation;
    this.tokensIterator = createTokenIterator();
    this.termAttr = addAttribute(CharTermAttribute.class);
    this.offsetAttr = addAttribute(OffsetAttribute.class);
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
    this.tokensIterator = createTokenIterator();
  }

  private Iterator<CoreLabel> createTokenIterator() {
    return AnnotationUtils.tokens(annotation).iterator();
  }
}
