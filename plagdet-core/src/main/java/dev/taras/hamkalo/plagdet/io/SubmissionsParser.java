package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;
import dev.taras.hamkalo.plagdet.io.exception.ParsingException;
import dev.taras.hamkalo.plagdet.io.reader.ContentReader;
import dev.taras.hamkalo.plagdet.io.reader.ContentReaders;
import dev.taras.hamkalo.plagdet.io.reader.MetadataReader;
import dev.taras.hamkalo.plagdet.io.reader.MetadataReaders;
import edu.stanford.nlp.util.Sets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionsParser {

  public static final Pattern SUBMITTER_REGEX = Pattern.compile(
    "\\d{4}_\\d{4}_([A-Z][a-z]+_[A-Z][a-z]+)_"
  );

  public static final Set<String> SUPPORTED_ARCHIVE_EXT = Set.of("zip");

  public static final Set<String> SUPPORTED_FILE_FORMATS = Set.of("docx", "doc", "txt", "pdf");

  ContentReader contentReader;

  MetadataReader metadataReader;

  public SubmissionsParser(List<ContentReader> contentReaders,
                           List<MetadataReader> metaReaders) {

    this.contentReader = new ContentReaders(contentReaders);
    this.metadataReader = new MetadataReaders(metaReaders);
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

  public List<Submission> parseSubmissions(String inputPath) throws ParsingException {
    var path = Path.of(inputPath);
    var file = path.toFile();
    if (!file.exists()) {
      throw new ParsingException("Path does not exist");
    }

    final String persistentPrefix = FilenameUtils.getBaseName(file.getName());
    if (isSupportedArchive(file)) {
      final var extractedEntries = extractArchiveSingleLevel(
        file, Sets.union(SUPPORTED_ARCHIVE_EXT, SUPPORTED_FILE_FORMATS)
      );
      return parseSubmissions(extractedEntries, persistentPrefix);
    }

    if (file.isDirectory()) {
      // add all paths inside of it, for a file we create a submission directly for it
      // and for a zip we extract all of them
      Map<String, File> fileMap = new HashMap<>();
      File[] childs = file.listFiles();
      if (childs != null) {
        for (var child : childs) {
          fileMap.put(child.getName(), child);
        }

        return parseSubmissions(fileMap, persistentPrefix);
      }

    }

    throw new ParsingException("Path does not represent directory or archive");
  }

  private List<Submission> parseSubmissions(
    @NotNull Map<String, File> submissionFiles,
    @NotNull String persistentPrefix
  ) {

    var submissions = new ArrayList<Submission>();
    for (var submissionSource: submissionFiles.entrySet()) {
      var submission = new Submission();
      var submissionFile = submissionSource.getValue();
      var submitterName = extractSubmitter(submissionFile.toPath());
      submission.setPath(submissionFile.toPath());

      log.info("Parsing {}", submissionFile.getAbsolutePath());


      try {
        if (isSupportedArchive(submissionFile)) {
          var fileMap = extractArchiveSingleLevel(
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
      } catch (ContentReadException | MetadataReadException e) {
        log.error("Submission {} was discarded, reason {}", submissionFile, e.getMessage());
      }

    }

    return submissions;
  }

  private FileData buildFileData(
    @NotNull File file,
    @NotNull String submitterName,
    @NotNull String persistentPath
  ) {
    return FileData.builder()
      .submitter(submitterName)
      .persistentPath(persistentPath)
      .temporaryPath(file.toPath())
      .content(contentReader.getContent(file.toPath()))
      .metadata(metadataReader.getMetadata(file.toPath()))
      .build();
  }

  /**
   * @param file             denote archive to be expanded
   * @param preservedFormats formats which will be preserved, other dropped
   * @return archive entry name -> file map
   */
  private Map<String, File> extractArchiveSingleLevel(
    @NotNull File file,
    @Nullable Set<String> preservedFormats
  ) throws ParsingException {

    var result = new HashMap<String, File>();

    try (InputStream fis = new FileInputStream(file);
         BufferedInputStream bis = new BufferedInputStream(fis);
         ArchiveInputStream<ZipArchiveEntry> ais = new ZipArchiveInputStream(bis)) {

      Path tempDir = Files.createTempDirectory("extracted_");
      tempDir.toFile().deleteOnExit();

      Path extractDest = tempDir.resolve(FilenameUtils.getBaseName(file.getName()));
      extractDest.toFile().deleteOnExit();

      ArchiveEntry entry;
      while ((entry = ais.getNextEntry()) != null) {
        String baseName = FilenameUtils.getBaseName(entry.getName());
        if (entry.isDirectory() ||
            entry.getName().contains("../") ||
            baseName.startsWith(".")) {
          continue;
        }

        if (entry.getName().chars().filter(c -> c == '/').count() > 1) {
          continue;
        }

        if (preservedFormats != null &&
            !preservedFormats.contains(FilenameUtils.getExtension(entry.getName()))) {
          continue;
        }
        File newFile = new File(extractDest.toFile(), entry.getName());
        File parentFile = newFile.getParentFile();
        parentFile.mkdirs();
        parentFile.deleteOnExit();
        newFile.deleteOnExit();

        try (var fos = new FileOutputStream(newFile)) {
          IOUtils.copy(ais, fos);
        }

        result.put(entry.getName(), newFile);
      }

    } catch (IOException e) {
      throw new ParsingException(e);
    }

    return result;
  }

  private String extractSubmitter(Path path) {
    var matcher = SUBMITTER_REGEX.matcher(path.toString());
    if (matcher.find()) {
      return matcher.group(1);
    }

    return FileNameUtils.getBaseName(path);
  }

  private static boolean isSupportedArchive(File file) {
    return file.isFile() &&
           SUPPORTED_ARCHIVE_EXT.contains(FilenameUtils.getExtension(file.getName()));
  }


}
