package dev.taras.hamkalo.plagdet.preprocessing.filter;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StopWordsFilter extends TokenFilter {

  public static final Path STOP_WORDS_PATH = Path.of("app/src/main/resources/stopwords.txt");

  CharArraySet stopwordSet;

  public StopWordsFilter() {
    this.stopwordSet = loadStopwordSet();
  }

  private static CharArraySet loadStopwordSet() {
    try(var stopwords = new FileReader(STOP_WORDS_PATH.toFile())) {
      return WordlistLoader.getWordSet(stopwords);
    } catch (IOException ignore) {
      throw new RuntimeException(ignore);
    }

  }

  @Override
  protected boolean doValidate(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      return !stopwordSet.contains(token.getContent());
    }

    return true;
  }
}
