package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.Analyzer;
import dev.taras.hamkalo.plagdet.index.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.LinkRemover;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessors;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.SectionNumberingRemover;
import dev.taras.hamkalo.plagdet.preprocessing.filter.*;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.*;
import dev.taras.hamkalo.plagdet.readers.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

  @Bean
  public Analyzer analyzer(ContentReaders contentReaders,
                           Preprocessor preprocessor,
                           Transformer transformer,
                           TokenFilter filter,
                           MetaReader metaReader
  ) {
    return new Analyzer(contentReaders, preprocessor, transformer, filter, metaReader);
  }

  @Bean
  public Transformer transformer() {
    return Transformer.of(
      tagger(),
      lowercase(),
      stemmer(),
      diacriticsRemover()
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
  public StemmingTransformer stemmer() {
    return new StemmingTransformer();
  }

  @Bean
  public LowercaseTransformer lowercase() {
    return new LowercaseTransformer();
  }

  @Bean
  public DiacriticsRemover diacriticsRemover() {
    return new DiacriticsRemover();
  }

  @Bean
  public TokenTagger tagger() {
    return new TokenTagger();
  }

//  @Bean
//  public TokenPrinter printer() {
//    return new TokenPrinter();
//  }

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
  public WordDocumentReader wordDocumentReader() {
    return new WordDocumentReader();
  }

  @Bean
  public MetaReader metaReader() {
    return wordDocumentReader();
  }

  @Bean
  public TextReader textReader() {
    return new TextReader();
  }

  @Bean
  public LuceneIndex luceneIndex() {
    return new LuceneIndex();
  }

}
