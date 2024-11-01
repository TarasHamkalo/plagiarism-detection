package dev.taras.hamkalo.plagdet.transformation;

public class TokenPrinter extends Transformer {

  @Override
  protected Token doTransform(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      System.out.println(token);
    }
    return token;
  }

}
