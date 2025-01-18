package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.reader.exception.ContentReadException;

import org.apache.commons.io.FilenameUtils;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContentReaders implements ContentReader {

  @NonNull
  List<ContentReader> readers;

  @Override
  public String getContent(Path path) throws ContentReadException {

    String extension = FilenameUtils.getExtension(path.getFileName().toString());

    for (var reader : readers) {
      if (reader.supports(extension)) {
        return reader.getContent(path);
      }
    }

    throw new ContentReadException("Extension is not supported: " + extension);
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
