
package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
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

  Boolean filterStopwords;

  @NonFinal
  Iterator<CoreLabel> tokensIterator;

  CharTermAttribute termAttr;

  OffsetAttribute offsetAttr;

  public StanfordAdapterTokenizer(CoreDocument coreDocument, boolean filterStopwords) {
    this.coreDocument = coreDocument;
//    this.tokensIterator = coreDocument.tokens().iterator();
    this.tokensIterator = coreDocument.annotation()
      .get(PlagdetAnnotations.WordsAnnotation.class)
      .iterator();

    this.termAttr = addAttribute(CharTermAttribute.class);
    this.offsetAttr = addAttribute(OffsetAttribute.class);
    this.filterStopwords = filterStopwords;
  }

  @Override
  public boolean incrementToken() {
    if (!tokensIterator.hasNext()) {
        return false;
    }

    var token = next();
    if (token == null) {
      return false;
    }
    log.debug(token.word());

    termAttr.setEmpty().append(token.lemma());
    offsetAttr.setOffset(token.beginPosition(), token.endPosition());
    return true;
  }

  private CoreLabel next() {
    while (tokensIterator.hasNext()) {
      var token = tokensIterator.next();
      Boolean isStopWord = token.get(PlagdetAnnotations.StopWordAnnotation.class);
      if (filterStopwords && isStopWord != null && isStopWord) {
        continue;
      }

      return token;
    }

    return null;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    this.tokensIterator = coreDocument.tokens().iterator();
  }
}
