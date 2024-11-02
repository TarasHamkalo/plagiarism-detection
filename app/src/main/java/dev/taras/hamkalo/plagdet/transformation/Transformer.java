package dev.taras.hamkalo.plagdet.transformation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// TODO: stop words removal
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class Transformer {

  Transformer next;

  public static Transformer of(Transformer first, Transformer... other) {
    var head = first;
    for (var transformer: other) {
      head.setNext(transformer);
      head = transformer;
    }

    return first;
  }

  public Token transform(Token token) {
    var newToken = doTransform(token);
    if (next != null) {
      return next.transform(newToken);
    }

    return newToken;
  }

  protected abstract Token doTransform(Token token);

}
