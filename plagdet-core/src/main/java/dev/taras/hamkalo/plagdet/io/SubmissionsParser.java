package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.io.exception.DocumentReadException;
import dev.taras.hamkalo.plagdet.io.exception.ParsingException;
import dev.taras.hamkalo.plagdet.io.reader.DocumentReader;
import dev.taras.hamkalo.plagdet.io.reader.DocumentReaders;
import edu.stanford.nlp.util.Sets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionsParser {

  public static final Pattern SUBMITTER_REGEX = Pattern.compile(
    "\\d{4}_\\d{4}_([A-Z][a-z]+)_([A-Z][a-z]+)_"
  );

  public static final Set<String> SUPPORTED_ARCHIVE_EXT = Set.of("zip");

  public static final Set<String> SUPPORTED_FILE_FORMATS = Set.of("docx", "doc", "pdf");

  DocumentReaders documentReaders;

  public SubmissionsParser(@NotNull List<DocumentReader> contentReaders) {
    this.documentReaders = new DocumentReaders(contentReaders);
  }

  public @NotNull List<Submission> parseSubmissions(
    @NotNull String inputPath
  ) throws ParsingException {
    var path = Path.of(inputPath);
    var file = path.toFile();
    if (!file.exists()) {
      throw new ParsingException("Path does not exist");
    }

    String persistentPrefix = FilenameUtils.getBaseName(file.getName());
    if (isSupportedArchive(file)) {
      return parseSubmissionsFromArchive(file, persistentPrefix);
    } else if (file.isDirectory()) {
      return parseSubmissionsFromDirectory(file, persistentPrefix);
    }

    throw new ParsingException("Path does not represent directory or archive");
  }

  private @NotNull List<Submission> parseSubmissionsFromArchive(
    @NotNull File file,
    @NotNull String persistentPrefix
  ) throws ParsingException {
    var extractedFiles = ArchiveExtractor.extract(
      file, Sets.union(SUPPORTED_ARCHIVE_EXT, SUPPORTED_FILE_FORMATS)
    );
    return parseSubmissions(extractedFiles, persistentPrefix);
  }

  private @NotNull List<Submission> parseSubmissionsFromDirectory(
    @NotNull File directory,
    @NotNull String persistentPrefix
  ) {
    Map<String, File> fileMap = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
      .collect(Collectors.toMap(File::getName, f -> f));
    return parseSubmissions(fileMap, persistentPrefix);
  }

  private List<Submission> parseSubmissions(
    @NotNull Map<String, File> submissionFiles,
    @NotNull String persistentPrefix
  ) {

    var submissions = new ArrayList<Submission>();
    for (var submissionSource : submissionFiles.entrySet()) {
      var submissionFile = submissionSource.getValue();
      var submitterName = extractSubmitter(submissionFile.toPath());
      var submission = new Submission(submissionFile.toPath(), new ArrayList<>());
      log.info("Parsing {}", submissionFile.getAbsolutePath());
      try {
        if (isSupportedArchive(submissionFile)) {
          var fileMap = ArchiveExtractor.extract(
            submissionFile, SubmissionsParser.SUPPORTED_FILE_FORMATS
          );

          var extendedPrefix =
            persistentPrefix + File.separator + FilenameUtils.getBaseName(submissionSource.getKey());

          var documents = fileMap.entrySet().stream()
            .map((e) ->
              buildFileData(e.getValue(), submitterName, extendedPrefix + File.separator + e.getKey())
            )
            .map(f -> DocumentEntry.builder().fileData(f).build())
            .toList();

          submission.setDocuments(documents);
        } else {
          final var persistentPath = persistentPrefix + File.separator + submissionSource.getKey();
          var fileData = buildFileData(submissionFile, submitterName, persistentPath);
          submission.setDocuments(List.of(DocumentEntry.builder().fileData(fileData).build()));
        }

        submissions.add(submission);
      } catch (ParsingException | DocumentReadException e) {
        log.error("Submission {} was discarded, reason {}", submissionFile, e.getMessage());
      }

    }

    return submissions;
  }

  public @Nullable DocumentEntry readTemplate(@Nullable String inputPath) throws ParsingException {
    if (inputPath == null) {
      return null;
    }

    var path = Path.of(inputPath);
    if (!Files.exists(path) || Files.isDirectory(path)) {
      throw new ParsingException("Path does not exist");
    }

    var fileData = buildFileData(path.toFile(), "template", "template");
    fileData.setSubmitter(extractSubmitter(path));
    return DocumentEntry.builder()
      .indexed(false)
      .template(true)
      .fileData(fileData)
      .build();
  }

  private static boolean isSupportedArchive(@NotNull File file) {
    return SUPPORTED_ARCHIVE_EXT.contains(FilenameUtils.getExtension(file.getName())) &&
           file.isFile();
  }

  private String extractSubmitter(Path path) {
    var matcher = SUBMITTER_REGEX.matcher(path.toString());
    if (matcher.find()) {
      return matcher.group(1) + " " + matcher.group(2);
    }

    return FileNameUtils.getBaseName(path);
  }

  private FileData buildFileData(
    @NotNull File file,
    @NotNull String submitterName,
    @NotNull String persistentPath
  ) {
    var fileData = documentReaders.getFileData(file.toPath());
    fileData.setSubmitter(submitterName);
    fileData.setPersistentPath(persistentPath);
    fileData.setTemporaryPath(file.toPath());
    return fileData;
  }
}