package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class TokenFilter {

  TokenFilter next;

  public static TokenFilter of(TokenFilter first, TokenFilter... other) {
    var head = first;
    for (var filter : other) {
      head.setNext(filter);
      head = filter;
    }

    return first;
  }

  /**
   * @param token
   * @return true if token is valid
   */
  public boolean validate(Token token) {
    if (!doValidate(token)) {
      return false;
    }

    if (next != null) {
      return next.validate(token);
    }

    return true;
  }

  protected abstract boolean doValidate(Token token);

}