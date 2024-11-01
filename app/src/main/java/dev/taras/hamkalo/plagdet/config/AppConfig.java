package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.Analyzer;
import dev.taras.hamkalo.plagdet.preprocessing.LinkRemover;
import dev.taras.hamkalo.plagdet.preprocessing.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.Preprocessors;
import dev.taras.hamkalo.plagdet.preprocessing.SectionNumberingRemover;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.readers.ContentReaders;
import dev.taras.hamkalo.plagdet.readers.TextReader;
import dev.taras.hamkalo.plagdet.transformation.TokenPrinter;
import dev.taras.hamkalo.plagdet.transformation.TokenTagger;
import dev.taras.hamkalo.plagdet.transformation.Transformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

  @Bean
  public Analyzer analyzer(ContentReaders contentReaders,
                           Preprocessor preprocessor,
                           Transformer transformer
  ) {
    return new Analyzer(contentReaders, preprocessor, transformer);
  }

  @Bean
  public Transformer transformer() {
    return Transformer.of(tagger(), printer());
  }

  @Bean
  public TokenTagger tagger() {
    return new TokenTagger();
  }

  @Bean
  public TokenPrinter printer() {
    return new TokenPrinter();
  }

  @Bean
  public Preprocessor preprocessor(Preprocessor... preprocessors) {
    return new Preprocessors(List.of(preprocessors));
  }

  @Bean
  public SectionNumberingRemover sectionNumberingRemover() {
    return new SectionNumberingRemover(true, true);
  }

  @Bean
  public LinkRemover linkRemover() {
    return new LinkRemover();
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
