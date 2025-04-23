package dev.taras.hamkalo.plagdet.io.filedata;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class LazyFileDataSource extends EagerFileDataSource {

  public LazyFileDataSource(
    @NotNull List<FileDataReader> contentReaders,
    @NotNull String inputPath
  ) throws FileDataSourceException {
    super(contentReaders, inputPath);
  }

  @Override
  public @NotNull FileData next() throws FileDataSourceException {
    var next = super.next();
    var fullFileData = fileDataReaders.getFileData(next.getTemporaryPath());
    fullFileData.setSubmitter(next.getSubmitter());
    fullFileData.setPersistentPath(next.getPersistentPath());
    fullFileData.setTemporaryPath(next.getTemporaryPath());
    return fullFileData;
  }

  @Override
  protected @NotNull FileData buildFileData(
    @NotNull File file,
    @NotNull String submitterName,
    @NotNull String persistentPath
  ) {
    return FileData.builder()
      .submitter(submitterName)
      .persistentPath(persistentPath)
      .temporaryPath(file.toPath())
      .build();
  }
}
