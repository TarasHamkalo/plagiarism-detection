package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;

public class SpecialCharacterFilter extends TokenFilter {

  @Override
  protected boolean doValidate(Token token) {
    return !Token.TokenType.SPECIAL.equals(token.getType());
  }

}