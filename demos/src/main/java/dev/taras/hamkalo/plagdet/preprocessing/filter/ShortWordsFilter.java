package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShortWordsFilter extends TokenFilter {

  int minLength = 4;

  @Override
  protected boolean doValidate(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      return token.getContent().length() >= minLength;
    }

    return true;
  }
}
