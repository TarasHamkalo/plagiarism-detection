package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;

import java.nio.file.Path;

public interface ContentReader {

  String getContent(Path path) throws ContentReadException;

  boolean supports(String extension);

}
