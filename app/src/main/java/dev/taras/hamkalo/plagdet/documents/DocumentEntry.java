package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Collectors;

// TODO: make clean method to remove content strings after transformation
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntry {

  String contentPath;

  String content;

  List<Token> tokens;

  Metadata metadata;

  public DocumentEntry(String contentPath) {
    this.contentPath = contentPath;
  }

  public String getContentFromTokens() {
    return tokens == null ? "" : tokens.stream().map(Token::getContent).collect(Collectors.joining(" "));
  }
}
