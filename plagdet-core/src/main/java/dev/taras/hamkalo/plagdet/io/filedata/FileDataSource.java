package dev.taras.hamkalo.plagdet.io.filedata;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FileDataSource {

  @NotNull List<FileData> all() throws FileDataSourceException;

  @NotNull FileData next() throws FileDataSourceException;

  int estimateFilesCount() throws FileDataSourceException;

  boolean hasNext() throws FileDataSourceException;
}
