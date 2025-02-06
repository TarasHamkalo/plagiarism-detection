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
      if (strings.length > 1) {
        analyzer.compare(strings[0], strings[1]);
      }

    };
  }
}
