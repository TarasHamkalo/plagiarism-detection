package dev.taras.hamkalo.plagdet.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class Utils {

  public static List<String> expand(String dirString) throws IOException {
    // Convert wildcard path to absolute and parse parent directory
    Path dir = Paths.get(dirString);
    List<String> resultPaths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      var iterator = stream.iterator();
      while (iterator.hasNext()) {
        resultPaths.add(iterator.next().toString());
      }
    }

    return resultPaths;
  }

}
