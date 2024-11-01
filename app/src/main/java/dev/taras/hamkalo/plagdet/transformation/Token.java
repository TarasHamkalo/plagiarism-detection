package dev.taras.hamkalo.plagdet.transformation;

import lombok.AccessLevel;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;

@With
@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Token {

  public enum TokenType {
    PUNCT, SPECIAL, TEXT, NUMBER, UNDEFINED
  }

  String content;

  TokenType type;

  Integer start;

  Integer end;

}
