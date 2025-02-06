package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;

public class PunctuationFilter extends TokenFilter {

  @Override
  protected boolean doValidate(Token token) {
    return !Token.TokenType.PUNCT.equals(token.getType());
  }

}
