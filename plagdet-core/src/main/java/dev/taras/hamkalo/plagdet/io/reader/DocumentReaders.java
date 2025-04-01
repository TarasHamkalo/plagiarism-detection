package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DocumentReaders implements DocumentReader {

  @NonNull
  List<DocumentReader> readers;

  @Override
  public FileData getFileData(Path path) throws DocumentReadException {
    String extension = FilenameUtils.getExtension(path.getFileName().toString());

    for (var reader : readers) {
      if (reader.supports(extension)) {
        return reader.getFileData(path);
      }
    }

    throw new DocumentReadException("Extension is not supported: " + extension);
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
