package dev.taras.hamkalo.plagdet.readers;

import java.nio.file.Path;

public interface ContentReader {

  String getContent(Path path) throws ContentReadException;

  boolean supports(String extension);

}
