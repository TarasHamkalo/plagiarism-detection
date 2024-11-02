package dev.taras.hamkalo.plagdet.preprocessing.transformation;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowercaseTransformer extends Transformer {

  @Override
  protected Token doTransform(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      return token.withContent(token.getContent().toLowerCase());
    }

    return token;
  }

}
