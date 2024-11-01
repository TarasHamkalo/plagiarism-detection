package dev.taras.hamkalo.plagdet.readers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class TextReader implements ContentReader {

  @Override
  public String getContent(Path path) throws ContentReadException {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }

  }

  @Override
  public boolean supports(String extension) {
    // TODO: matches any
    return Set.of("txt", "tex", "").contains(extension.toLowerCase());
  }

}