package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;

import java.nio.file.Path;

public interface DocumentReader {

  FileData getFileData(Path path) throws DocumentReadException;

  boolean supports(String extension);

}
