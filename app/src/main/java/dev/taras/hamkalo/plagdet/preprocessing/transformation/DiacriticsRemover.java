package dev.taras.hamkalo.plagdet.preprocessing.transformation;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import org.apache.commons.lang3.StringUtils;

public class DiacriticsRemover extends Transformer {

  @Override
  protected Token doTransform(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      return token.withContent(StringUtils.stripAccents(token.getContent()));
    }

    return token;
  }
}
