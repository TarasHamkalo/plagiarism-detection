
package dev.taras.hamkalo.plagdet.index.lucene;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenListTokenizer extends Tokenizer {

  List<Token> tokens;

  @NonFinal
  int index = 0;

  CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

  OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

  public TokenListTokenizer(List<Token> tokens) {
    this.tokens = tokens;
  }

  @Override
  public boolean incrementToken() {
    if (index >= tokens.size()) {
      return false;
    }

    var token = tokens.get(index++);
    termAttr.setEmpty().append(token.getContent());
    offsetAttr.setOffset(token.getStart(), token.getEnd());
    return true;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    index = 0;
  }

  @Override
  public void end() throws IOException {
    super.end();
    index = 0;
  }
}
