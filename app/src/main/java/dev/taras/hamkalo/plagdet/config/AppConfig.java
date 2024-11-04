package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.Analyzer;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.*;
import dev.taras.hamkalo.plagdet.preprocessing.filter.*;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.*;
import dev.taras.hamkalo.plagdet.readers.*;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;

@Configuration
public class AppConfig {

  @Bean
  public LuceneIndex luceneIndex() {
    return new LuceneIndex();
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
