package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;

public class NumberFilter extends TokenFilter {

  @Override
  protected boolean doValidate(Token token) {
    return !Token.TokenType.NUMBER.equals(token.getType());
  }

}