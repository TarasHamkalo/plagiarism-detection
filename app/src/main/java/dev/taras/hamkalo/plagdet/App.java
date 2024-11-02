package dev.taras.hamkalo.plagdet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class App {

  private static final List<String> PATHS = List.of(
//    "app/src/main/resources/10.txt",
//    "app/src/main/resources/11.txt"
    "/home/taras-hamkalo/repositories/plagiarism-detection/app/src/main/resources/20.docx"
  );

  public static void main(String[] args) {
    new SpringApplication(App.class).run(args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(Analyzer analyzer) {
    return strings -> {
      List<String> expandedPaths = expand("/home/taras-hamkalo/other/data/23-24-ipc-docx/sk");
      System.out.println(analyzer.analyze(expandedPaths));
//      var path = "/home/taras-hamkalo/other/data/23-24-ipc-docx/sk/2016-web-posted.docx";
//      analyzer.findSimilarTo(path);
    };
  }

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
