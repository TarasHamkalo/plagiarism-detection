package dev.taras.hamkalo.plagdet.transformation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

@Slf4j
public class TokenTagger extends Transformer {

  @Override
  protected Token doTransform(Token token) {
    return token.withType(classifyToken(token.getContent()));
  }

  private static Token.TokenType classifyToken(String token) {
    var normalized = StringUtils.stripAccents(token);

    if (normalized.matches("\\p{Punct}*")) {
      return Token.TokenType.PUNCT;
    } else if (NumberUtils.isParsable(normalized)) {
      return Token.TokenType.NUMBER;
    } else if (normalized.matches("\\w+")) {
      return Token.TokenType.TEXT;
    }

    return Token.TokenType.SPECIAL;
  }

}
