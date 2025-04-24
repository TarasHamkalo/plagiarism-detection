package dev.taras.hamkalo.plagdet.io.filedata;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReader;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LazyFileDataSource extends EagerFileDataSource {

  @Nullable FileData cachedFileData;

  public LazyFileDataSource(
    @NotNull List<FileDataReader> contentReaders,
    @NotNull String inputPath
  ) throws FileDataSourceException {
    super(contentReaders, inputPath);
  }

  @Override
  public boolean hasNext() throws FileDataSourceException {
    if (cachedFileData != null) {
      return true;
    }

    while (super.hasNext()) {
      var next = super.next();
      try {
        this.cachedFileData = tryToReadFileData(next);
        return true;
      } catch (FileDataSourceException | DocumentReadException e) {
        log.error("Submission [{}] was discarded, reason [{}]", next.getPersistentPath(), e.getMessage());
      }
    }

    return false;
  }

  @Override
  public @NotNull FileData next() throws FileDataSourceException {
    if (cachedFileData != null) {
      var temp = cachedFileData;
      this.cachedFileData = null;
      return temp;
    }

    throw new FileDataSourceException("hasNext was never called to load file data");
  }

  private @NotNull FileData tryToReadFileData(FileData next) {
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
