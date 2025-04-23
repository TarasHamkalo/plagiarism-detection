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

// a.zip -> {file.docx, file.pdf, file.doc}
// a.zip -> {a.zip, b.zip, c.zip}
// single.docx | single.pdf | single.doc
//
@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class EagerFileDataSource implements FileDataSource {

  public static final Set<String> SUPPORTED_ARCHIVE_EXT = Set.of("zip");

  public static final Set<String> SUPPORTED_FILE_FORMATS = Set.of("docx", "doc", "pdf");

  @NotNull
  FileDataReaders fileDataReaders;

  @NotNull
  Queue<FileData> parsedFileData;

  public EagerFileDataSource(
    @NotNull List<FileDataReader> contentReaders,
    @NotNull String inputPath
  ) throws FileDataSourceException {
    this.fileDataReaders = new FileDataReaders(contentReaders);
    log.info("Parsing path {} started", inputPath);
    this.parsedFileData = loadFileData(inputPath);
    log.info("Parsing path {} done", inputPath);
  }

  protected @NotNull Queue<FileData> loadFileData(@NotNull String inputPath) {
    return new LinkedList<>(parseSubmissions(inputPath));
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
    } else if (IoUtils.isExtensionSupported(file, SUPPORTED_FILE_FORMATS)) {
      return parseSubmissions(Map.of(file.getName(), file), persistentPrefix);
    }

    throw new FileDataSourceException("FS entry provided does not represent a valid input");
  }

  private @NotNull List<FileData> parseSubmissionsFromArchive(
    @NotNull File file,
    @NotNull String persistentPrefix
  ) throws FileDataSourceException {
    var extractedFiles = ArchiveExtractor.extract(
      file, Sets.union(SUPPORTED_ARCHIVE_EXT, SUPPORTED_FILE_FORMATS)
    );
    assertValidFileMap(extractedFiles);
    return parseSubmissions(extractedFiles, persistentPrefix);
  }

  private @NotNull List<FileData> parseSubmissionsFromDirectory(
    @NotNull File directory,
    @NotNull String persistentPrefix
  ) {
    Map<String, File> fileMap = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
      .collect(Collectors.toMap(File::getName, f -> f));

    assertValidFileMap(fileMap);

    return parseSubmissions(fileMap, persistentPrefix);
  }

  private void assertValidFileMap(@NotNull Map<String, File> fileMap) {
    if (fileMap.values().stream().allMatch(File::isDirectory)) {
      throw new FileDataSourceException(
        "FS entry provided, holds directories, recursive lookup won't be made."
      );
    }

    if (!areSubmittedFilesConsistent(fileMap)) {
      throw new FileDataSourceException(
        "Files provided are not in the consistent form (directories/archives/plain files are mixed)"
      );
    }
  }

  private boolean areSubmittedFilesConsistent(
    @NotNull Map<String, File> submittedFiles
  ) {

    var extensions = submittedFiles.values().stream()
      .map(f -> f.isDirectory() ? "dir" : FilenameUtils.getExtension(f.getName()))
      .toList();

    return SUPPORTED_ARCHIVE_EXT.containsAll(extensions) ||
           SUPPORTED_FILE_FORMATS.containsAll(extensions);
  }

  private @NotNull List<FileData> parseSubmissions(
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

          if (fileMap.isEmpty()) {
            log.warn("Submitter [{}] does not have any valid file.", submitterName);
          }

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

  protected @NotNull FileData buildFileData(
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