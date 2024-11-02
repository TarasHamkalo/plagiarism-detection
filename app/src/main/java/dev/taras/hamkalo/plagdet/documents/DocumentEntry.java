package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

// TODO: make clean method to remove content strings after transformation
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentEntry {

  Long id;

  Path path;

  @NonFinal
  String content;

  @NonFinal
  String normalizedContent;

  @NonFinal
  List<Token> tokens;

  @NonFinal
  Metadata metadata;

  public DocumentEntry(String path) {
    this.id = (long) path.hashCode();
    this.path = Path.of(path);
  }

  // TODO: store result
  public String getContentFromTokens() {
    return tokens.stream().map(Token::getContent).collect(Collectors.joining(" "));
  }
}
