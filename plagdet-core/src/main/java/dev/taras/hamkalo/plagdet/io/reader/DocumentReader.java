package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;

import java.nio.file.Path;

public interface DocumentReader {

  String getContent(Path path) throws ContentReadException;

  Metadata getMetadata(Path path) throws MetadataReadException;

  boolean supports(String extension);

}
