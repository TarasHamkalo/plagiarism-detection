package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReader;
import dev.taras.hamkalo.plagdet.io.filedata.reader.FileDataReaders;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class IoUtils {


  public static final Pattern SUBMITTER_REGEX = Pattern.compile(
    "\\d{4}_\\d{4}_([A-Z][a-z]+)_([A-Z][a-z]+)_"
  );

  public static List<Path> expand(String dirString) throws IOException {
    // Convert wildcard path to absolute and parse parent directory
    Path dir = Paths.get(dirString);
    List<Path> resultPaths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        resultPaths.add(path);
      }
    }

    return resultPaths;
  }

  public static @NotNull String resourceContent(@NotNull String resource) {
    try (var resourceAsStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
      return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static @NotNull Path resourceFromTemp(@NotNull String resource, @NotNull String filename) {
    try (var resourceAsStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
      Path tempDir = Files.createTempDirectory("dir_");
      Path tempFilePath = tempDir.resolve(filename);
      Files.copy(resourceAsStream, tempFilePath);
      tempDir.toFile().deleteOnExit();
      tempFilePath.toFile().deleteOnExit();
      return tempFilePath;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static @NotNull Path resolveFile(
    @NotNull Path rootDir,
    @NotNull String fileName,
    boolean removeOnExit
  ) {
    Path path = rootDir.resolve(fileName);
    if (removeOnExit) {
      path.toFile().deleteOnExit();
    }

    return path;
  }

  public static @NotNull Path resolveDirectory(
    @NotNull Path rootDir,
    @NotNull String directoryName,
    boolean removeOnExit
  ) throws IOException {
    Path directoryPath = rootDir.resolve(directoryName);
    Files.createDirectories(directoryPath);
    if (removeOnExit) {
      directoryPath.toFile().deleteOnExit();
    }
    return directoryPath;
  }

  public static boolean isExtensionSupported(
    @NotNull File file,
    @NotNull Set<String> supportedExtensions
  ) {
    String extension = FilenameUtils.getExtension(file.getName());
    return supportedExtensions.contains(extension) && file.isFile();
  }

  public static @NotNull String extractSubmitter(@NotNull Path path) {
    var matcher = SUBMITTER_REGEX.matcher(path.toString());
    if (matcher.find()) {
      return matcher.group(1) + " " + matcher.group(2);
    }

    return FileNameUtils.getBaseName(path);
  }

  public static @NotNull String getFileContent(
    @NotNull List<FileDataReader> readersList,
    @NotNull String pathString
  ) {
    Path path = Path.of(pathString);
    File file = path.toFile();
    if (file.isFile()) {
      FileDataReaders readers = new FileDataReaders(readersList);
      var fileData = readers.getFileData(path);
      return fileData.getContent();
    } else {
      throw new IllegalArgumentException("Submitted pass is not a file");
    }
  }

}
