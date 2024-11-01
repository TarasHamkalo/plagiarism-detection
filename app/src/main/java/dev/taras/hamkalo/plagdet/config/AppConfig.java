package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.Analyzer;
import dev.taras.hamkalo.plagdet.preprocessing.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.Preprocessors;
import dev.taras.hamkalo.plagdet.preprocessing.SectionNumberingRemover;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.readers.ContentReaders;
import dev.taras.hamkalo.plagdet.readers.TextReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

  @Bean
  public Analyzer analyzer(ContentReaders contentReaders, Preprocessor preprocessor) {
    return new Analyzer(contentReaders, preprocessor);
  }

  @Bean
  public Preprocessor preprocessor(Preprocessor preprocessors) {
    return new Preprocessors(List.of(preprocessors));
  }

  @Bean
  public SectionNumberingRemover sectionNumberingRemover() {
    return new SectionNumberingRemover(true, true);
  }

  @Bean
  public ContentReaders contentReaders(ContentReader... contentReaders) {
    return new ContentReaders(List.of(contentReaders));
  }

  @Bean
  public TextReader textReader() {
    return new TextReader();
  }

}
