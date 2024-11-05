package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.util.Utils;
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
//      List<String> expandedPaths = Utils.expand("/home/taras-hamkalo/other/data/23-24-ipc-docx/sk");

//      var expandedPaths = Utils.expand("/home/taras-hamkalo/other/data/ipc/meged-before-23-24/docx");
//      System.out.println(analyzer.addToIndex(expandedPaths));

//      var path = "/home/taras-hamkalo/other/data/23-24-ipc-docx/sk/2016-web-posted.docx";
//      path = "SIMPLE_FS_INDEX/-938587277.docx";
//      analyzer.findSimilarTo(path);
      var start = System.nanoTime();
      var path = "/home/taras-hamkalo/other/data/ipc/23-24/text/3465931603.txt";
      analyzer.findSimilarTo(path);
      System.out.println((System.nanoTime() - start) * 1e-9);
//      System.out.println(">>>> " + path.hashCode());

    };
  }
}
