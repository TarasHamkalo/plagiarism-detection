package dev.taras.hamkalo.plagdet.preprocessing.transformation;

import dev.taras.hamkalo.plagdet.preprocessing.Token;

public class TokenPrinter extends Transformer {

  @Override
  protected Token doTransform(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      System.out.println(token);
    }
    return token;
  }

}
