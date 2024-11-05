package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.analyzers.LuceneAnalyzer;
import dev.taras.hamkalo.plagdet.analyzers.MetaAnalyzer;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.FileSystemStore;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.index.metadata.DocumentRepository;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.NopPreprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.filter.*;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.*;
import dev.taras.hamkalo.plagdet.readers.*;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Configuration
public class AppConfig {

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
  public MetaAnalyzer metaAnalyzer(MetadataIndex metadataIndex,
                                   DocumentProcessor documentProcessor
  ) {
    return new MetaAnalyzer(metadataIndex, documentProcessor);
  }

  @Bean
  public LuceneAnalyzer luceneAnalyzer(LuceneIndex luceneIndex,
                                       DocumentProcessor documentProcessor
  ) {
    return new LuceneAnalyzer(luceneIndex, documentProcessor);
  }
//  @Bean
//  public IndexSequence indexSequence(FileSystemStore fileSystemIndex, MetadataIndex metadataIndex) {
//    var pushSequence = List.of(
//      fileSystemIndex,
//      metadataIndex,
//    );
//
//    return new IndexSequence(fileSystemIndex, pushSequence.subList(1, 3), pushSequence);
//  }

  @Bean
  public LuceneIndex luceneIndex() throws IOException {
    return new LuceneIndex();
  }

  @Bean
  public MetadataIndex metadataIndex(DocumentRepository documentRepository) {
    return new MetadataIndex(documentRepository);
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
