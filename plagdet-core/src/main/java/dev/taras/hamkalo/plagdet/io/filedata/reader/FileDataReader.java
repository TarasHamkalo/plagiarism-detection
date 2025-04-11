package dev.taras.hamkalo.plagdet.io.filedata.reader;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;

import java.nio.file.Path;

public interface FileDataReader {

  FileData getFileData(Path path) throws DocumentReadException;

  boolean supports(String extension);

}
