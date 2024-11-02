package dev.taras.hamkalo.plagdet.transformation;

import dev.taras.hamkalo.plagdet.transformation.stemmers.EssentialDataStemmer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StemmingTransformer extends Transformer {

  EssentialDataStemmer stemmer = new EssentialDataStemmer();

  @Override
  protected Token doTransform(Token token) {
    if (!Token.TokenType.TEXT.equals(token.getType())) {
      return token;
    }

    String content = token.getContent();
    int len = stemmer.stem(content.toCharArray(), content.length());
    return token.withContent(content.substring(0, len));
  }

}
