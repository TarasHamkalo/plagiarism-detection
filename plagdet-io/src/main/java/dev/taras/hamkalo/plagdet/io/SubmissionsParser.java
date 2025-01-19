package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;
import dev.taras.hamkalo.plagdet.io.exception.ParsingException;
import dev.taras.hamkalo.plagdet.io.reader.ContentReader;
import dev.taras.hamkalo.plagdet.io.reader.ContentReaders;
import dev.taras.hamkalo.plagdet.io.reader.MetaReader;
import dev.taras.hamkalo.plagdet.io.reader.MetaReaders;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionsParser {

  public static final Pattern SUBMITTER_REGEX = Pattern.compile(
    "\\d{4}_\\d{4}_([A-Z][a-z]+_[A-Z][a-z]+)_"
  );

  public static final String DEFAULT_ARCHIVE_EXT = ".zip";

  public static final Set<String> SUPPORTED_FILE_FORMATS = Set.of(".docx", ".doc");

  ContentReader contentReader;

  MetaReader metadataReader;

  public SubmissionsParser(List<ContentReader> contentReaders,
                           List<MetaReader> metaReaders) {

    this.contentReader = new ContentReaders(contentReaders);
    this.metadataReader = new MetaReaders(metaReaders);
  }

  public List<Submission> parseSubmissions(String strPath) throws ParsingException {
    var path = Path.of(strPath);
    var file = path.toFile();
    if (!file.exists()) {
      throw new ParsingException("Path does not exist");
    }

    List<File> submissionFiles = new ArrayList<>();
    if (isSupportedArchive(file)) {
      submissionFiles.addAll(extractArchiveSingleLevel(file, Set.of(DEFAULT_ARCHIVE_EXT)));
    }

    if (file.isDirectory()) {
      // add all paths inside of it, for a file we create a submission directly for it
      // and for a zip we extract all of them
      File[] childs = file.listFiles();
      if (childs != null) {
        submissionFiles.addAll(Arrays.asList(childs));
      }
    }

    var submissions = new ArrayList<Submission>();
    for (var submissionFile : submissionFiles) {
      log.debug(submissionFile.getAbsolutePath());
      var submission = new Submission();
      submission.setPath(submissionFile.toPath());

      var submitterName = extractSubmitter(file.toPath());
      try {
        if (isSupportedArchive(submissionFile)) {
          var files = extractArchiveSingleLevel(
            submissionFile, SubmissionsParser.SUPPORTED_FILE_FORMATS
          );
          submission.setFiles(files.stream()
            .map(this::readFileData)
            .peek(f -> f.setSubmitter(submitterName))
            .toList()
          );
        } else {
          var fileData = readFileData(submissionFile);
          fileData.setSubmitter(submitterName);
          submission.setFiles(List.of(fileData));
        }

        submissions.add(submission);
      } catch(ContentReadException | MetadataReadException e){
        log.error("Submission {} was discarded, reason {}", submissionFile, e.getMessage());
      }
    }

    return submissions;
  }

  private FileData readFileData(File file) {
    return (FileData.builder()
      .path(file.toPath())
      .content(contentReader.getContent(file.toPath()))
      .metadata(metadataReader.getMetadata(file.toPath()))
      .build()
    );
  }

  /**
   * @param file             denote archive to be expanded
   * @param preservedFormats formats which will be preserved, other dropped
   * @return File
   */
  private List<File> extractArchiveSingleLevel(File file, Set<String> preservedFormats)
    throws ParsingException {

    var result = new ArrayList<File>();

    try (var zis = new ZipInputStream(new FileInputStream(file), Charset.forName("cp437"))) {
      Path tempDir = Files.createTempDirectory(null);
      ZipEntry entry;
      byte[] buffer = new byte[2048];
      while ((entry = zis.getNextEntry()) != null) {
        File newFile = new File(tempDir.toFile(), entry.getName());
        if (entry.isDirectory() ||
            entry.getName().startsWith(".") ||
            entry.getName().contains("../") ||
            entry.getName().contains("/")) {
          continue;
        }

        if (preservedFormats != null &&
            preservedFormats.contains(FilenameUtils.getExtension(entry.getName()))) {
          continue;
        }

        try (var fos = new FileOutputStream(newFile)) {
          int length;
          while ((length = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
          }

        }

        result.add(newFile);
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

    return path.toString();
  }

  private static boolean isSupportedArchive(File file) {
    return file.isFile() && file.toPath().toString().endsWith(DEFAULT_ARCHIVE_EXT);
  }


}
