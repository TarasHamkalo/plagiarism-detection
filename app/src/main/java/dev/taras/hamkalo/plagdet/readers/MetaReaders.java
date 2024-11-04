package dev.taras.hamkalo.plagdet.readers;

import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.util.Utils;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class MetaReaders implements MetaReader {

  @NonNull
  List<MetaReader> readers;

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {

    String extension = Utils.getExtension(path);

    for (var reader : readers) {
      if (reader.supports(extension)) {
        return reader.getMetadata(path);
      }
    }

    throw new MetadataReadException("Extension is not supported: " + extension);
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
