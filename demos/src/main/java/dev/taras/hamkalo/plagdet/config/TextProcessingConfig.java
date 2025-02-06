package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.FileSystemStore;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.NopPreprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.io.*;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;

@Configuration
public class TextProcessingConfig {

  @Bean
  public DocumentProcessor documentProcessor(ContentReader contentReaders) {
    return new DocumentProcessor(
      contentReaders,
      metaReaders(),
      transformer(),
      filter(),
      pipeline()
    );
  }


  @Bean
  public Transformer transformer() {
    return Transformer.of(
      new TokenTagger(),
      new LowercaseTransformer(),
      new StemmingTransformer(),
      new DiacriticsRemover()
    );
  }

  @Bean
  public TokenFilter filter() {
    return TokenFilter.of(
      new PunctuationFilter(),
      new NumberFilter(),
      new ShortWordsFilter(),
      new SpecialCharacterFilter()
    );
  }

  @Bean
  public FileSystemStore fileSystemStore() {
    return new FileSystemStore();
  }

  @Bean
  public StanfordCoreNLP pipeline() {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize");
    return new StanfordCoreNLP(props);
  }

  @Bean
  public Preprocessor preprocessor(Preprocessor... preprocessors) {
    return new NopPreprocessor();
//    return new Preprocessors(List.of(preprocessors));
  }

  @Bean
  public ContentReaders contentReaders(ContentReader... contentReaders) {
    return new ContentReaders(List.of(contentReaders));
  }

  @Bean
  public WordDocumentReader wordDocumentReader() {
    return new WordDocumentReader();
  }

  @Bean
  public MetaReader metaReaders() {
    return new MetaReaders(List.of(wordDocumentReader()));
  }

  @Bean
  public TextReader textReader() {
    return new TextReader();
  }
}
