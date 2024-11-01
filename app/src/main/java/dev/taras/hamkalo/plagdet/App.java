package dev.taras.hamkalo.plagdet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class App {

  private static final List<String> PATHS = List.of(
    "app/src/main/resources/10.txt",
    "app/src/main/resources/11.txt"
  );

  public static void main(String[] args) {
    new SpringApplication(App.class).run(args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(Analyzer analyzer) {
    return strings -> {
      System.out.println(analyzer.analyze(PATHS));
    };
  }

}
