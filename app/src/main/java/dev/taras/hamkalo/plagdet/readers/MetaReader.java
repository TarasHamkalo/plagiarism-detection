package dev.taras.hamkalo.plagdet.readers;

import dev.taras.hamkalo.plagdet.documents.Metadata;

import java.nio.file.Path;

public interface MetaReader {

  Metadata getMetadata(Path path) throws MetadataReadException;

  boolean supports(String extension);

}
