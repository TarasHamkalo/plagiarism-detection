package dev.taras.hamkalo.plagdet.io.filedata;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.ArchiveExtractor;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReader;
import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReaders;
import edu.stanford.nlp.util.Sets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EagerFileDataSource implements FileDataSource {

  public static final Set<String> SUPPORTED_ARCHIVE_EXT = Set.of("zip");

  public static final Set<String> SUPPORTED_FILE_FORMATS = Set.of("docx", "doc", "pdf");

  @NotNull
  final FileDataReaders fileDataReaders;

  @NotNull
  final Queue<FileData> parsedFileData;

  public EagerFileDataSource(
    @NotNull List<FileDataReader> contentReaders,
    @NotNull String inputPath
  ) throws FileDataSourceException {
    this.fileDataReaders = new FileDataReaders(contentReaders);
    log.info("Parsing path {} started", inputPath);
    this.parsedFileData = new LinkedList<>(parseSubmissions(inputPath));
    log.info("Parsing path {} done", inputPath);
  }

  @Override
  public @NotNull List<FileData> all() throws FileDataSourceException {
    return new ArrayList<>(parsedFileData);
  }

  @Override
  public @NotNull FileData next() throws FileDataSourceException {
    FileData fileData = parsedFileData.poll();
    if (fileData != null) {
      return fileData;
    }
    throw new FileDataSourceException("FileData source is empty");
  }

  @Override
  public boolean hasNext() {
    return !parsedFileData.isEmpty();
  }

  public @NotNull List<FileData> parseSubmissions(
    @NotNull String inputPath
  ) throws FileDataSourceException {
    var path = Path.of(inputPath);
    var file = path.toFile();
    if (!file.exists()) {
      throw new FileDataSourceException("Path does not exist");
    }

    String persistentPrefix = FilenameUtils.getBaseName(file.getName());
    if (IoUtils.isExtensionSupported(file, SUPPORTED_ARCHIVE_EXT)) {
      return parseSubmissionsFromArchive(file, persistentPrefix);
    } else if (file.isDirectory()) {
      return parseSubmissionsFromDirectory(file, persistentPrefix);
    }

    throw new FileDataSourceException("Path does not represent directory or archive");
  }

  private @NotNull List<FileData> parseSubmissionsFromArchive(
    @NotNull File file,
    @NotNull String persistentPrefix
  ) throws FileDataSourceException {
    var extractedFiles = ArchiveExtractor.extract(
      file, Sets.union(SUPPORTED_ARCHIVE_EXT, SUPPORTED_FILE_FORMATS)
    );
    return parseSubmissions(extractedFiles, persistentPrefix);
  }

  private @NotNull List<FileData> parseSubmissionsFromDirectory(
    @NotNull File directory,
    @NotNull String persistentPrefix
  ) {
    Map<String, File> fileMap = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
      .collect(Collectors.toMap(File::getName, f -> f));
    return parseSubmissions(fileMap, persistentPrefix);
  }

  private List<FileData> parseSubmissions(
    @NotNull Map<String, File> submissionFiles,
    @NotNull String persistentPrefix
  ) {

    var parsedFileData = new ArrayList<FileData>();
    for (var submissionSource : submissionFiles.entrySet()) {
      var submissionFile = submissionSource.getValue();
      var submitterName = IoUtils.extractSubmitter(submissionFile.toPath());
      log.trace("Parsing {}", submissionFile.getAbsolutePath());
      try {
        if (IoUtils.isExtensionSupported(submissionFile, SUPPORTED_ARCHIVE_EXT)) {
          var fileMap = ArchiveExtractor.extract(
            submissionFile, EagerFileDataSource.SUPPORTED_FILE_FORMATS
          );

          var extendedPrefix =
            persistentPrefix + File.separator + FilenameUtils.getBaseName(submissionSource.getKey());

          parsedFileData.addAll(
            fileMap.entrySet().stream()
              .map((e) ->
                buildFileData(
                  e.getValue(), submitterName, extendedPrefix + File.separator + e.getKey()
                )
              )
              .toList()
          );
        } else {
          String persistentPath = persistentPrefix + File.separator + submissionSource.getKey();
          parsedFileData.add(buildFileData(submissionFile, submitterName, persistentPath));
        }
      } catch (FileDataSourceException | DocumentReadException e) {
        log.error("Submission {} was discarded, reason {}", submissionFile, e.getMessage());
      }

    }

    return parsedFileData;
  }

  private FileData buildFileData(
    @NotNull File file,
    @NotNull String submitterName,
    @NotNull String persistentPath
  ) {
    var fileData = fileDataReaders.getFileData(file.toPath());
    fileData.setSubmitter(submitterName);
    fileData.setPersistentPath(persistentPath);
    fileData.setTemporaryPath(file.toPath());
    return fileData;
  }
}