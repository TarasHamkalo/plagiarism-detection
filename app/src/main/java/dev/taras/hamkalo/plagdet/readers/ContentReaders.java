package dev.taras.hamkalo.plagdet.readers;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContentReaders implements ContentReader {

  @NonNull
  List<ContentReader> readers;

  @Override
  public String getContent(Path path) throws ContentReadException {

    String extension = getExtension(path);

    for (var reader : readers) {
      if (reader.supports(extension)) {
        return reader.getContent(path);
      }
    }

    throw new ContentReadException("Extension is not supported: " + extension);
  }

  private static String getExtension(Path path) {
    String extension;
    try {
      extension = FilenameUtils.getExtension(path.getFileName().toString());
    } catch (IllegalArgumentException e) {
      throw new ContentReadException("The path is invalid");
    }
    return extension;
  }

  @Override
  public boolean supports(String extension) {
    for (var reader : readers) {
      if (reader.supports(extension)) {
        return true;
      }
    }

    return false;
  }

}
