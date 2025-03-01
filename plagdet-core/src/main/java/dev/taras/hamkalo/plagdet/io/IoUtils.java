package dev.taras.hamkalo.plagdet.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class IoUtils {

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

  public static Path resourceFromTemp(@NotNull String resource, @NotNull String filename) {
    var resourceAsStream = ClassLoader.getSystemClassLoader()
      .getResourceAsStream(resource);

    try {
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


  public static @NotNull Path temporaryFile(
    @NotNull Path tempDir,
    @NotNull String fileName
  ) {
    Path path = tempDir.resolve(fileName);
    path.toFile().deleteOnExit();
    return path;
  }

  public static @NotNull Path temporaryDirectory(
    @NotNull Path rootDir,
    @NotNull String directoryName
  ) throws IOException {
    Path directoryPath = rootDir.resolve(directoryName);
    Files.createDirectories(directoryPath);
    directoryPath.toFile().deleteOnExit();
    return directoryPath;
  }


}
