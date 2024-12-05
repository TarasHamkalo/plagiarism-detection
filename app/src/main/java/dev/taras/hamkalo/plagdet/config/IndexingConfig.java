package dev.taras.hamkalo.plagdet.config;

import dev.taras.hamkalo.plagdet.analyzers.LuceneAnalyzer;
import dev.taras.hamkalo.plagdet.analyzers.MetaAnalyzer;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.index.metadata.DocumentRepository;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

//@Configuration
public class IndexingConfig {

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


}
