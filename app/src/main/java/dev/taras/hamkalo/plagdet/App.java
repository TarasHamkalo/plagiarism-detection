package dev.taras.hamkalo.plagdet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class App {

  public static void main(String[] args) {
    new SpringApplication(App.class).run(args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(Analyzer analyzer) {
    return strings -> {
//      List<String> expandedPaths = expand("/home/taras-hamkalo/other/data/23-24-ipc-docx/sk");
//      System.out.println(analyzer.analyze(expandedPaths));

      var path = "/home/taras-hamkalo/other/data/23-24-ipc-docx/sk/2016-web-posted.docx";
      analyzer.findSimilarTo(path);
    };
  }
}
