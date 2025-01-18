package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.Metadata;
import dev.taras.hamkalo.plagdet.io.reader.exception.MetadataReadException;

import java.nio.file.Path;

public interface MetaReader {

  Metadata getMetadata(Path path) throws MetadataReadException;

  boolean supports(String extension);

}
