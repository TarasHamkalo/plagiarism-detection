package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.DocumentIndex;
import dev.taras.hamkalo.plagdet.index.FileSystemIndex;
import dev.taras.hamkalo.plagdet.index.IndexingSequence;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.filter.TokenFilter;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.Transformer;
import dev.taras.hamkalo.plagdet.readers.MetaReader;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// TODO: normal implementation
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  List<DocumentProcessor> documentProcessors = new ArrayList<>();

  ContentReader contentReaders;

  Preprocessor preprocessor;

  Transformer transformer;

  TokenFilter tokenFilter;

  MetaReader metaReaders;

  StanfordCoreNLP pipeline;

  // TODO: can be local
  IndexingSequence indexingSequence;

  public void findSimilarTo(String path) {
    DocumentIndex documentIndex = new LuceneIndex();
    var docProc = createDocumentProcessor(path);

    var documentEntry = docProc
      .preprocess()
      .transform();

    documentIndex.retrieveSimilarByContent(documentEntry).forEach(System.out::println);
  }

  public String addToIndex(List<String> paths) {
    List<DocumentProcessor> documentProcessors = new ArrayList<>();
    for (String path : paths) {
      documentProcessors.add(
        createDocumentProcessor(path)
      );
    }

    List<DocumentEntry> documentEntries = new ArrayList<>();
    for (var documentProcessor : documentProcessors) {
      var documentEntry = documentProcessor
        .preprocess()
        .transform();

      documentEntries.add(documentEntry);

      indexingSequence.pushToIndex(documentEntry);
    }

    return "added to index " + documentEntries.size();
  }

  private DocumentProcessor createDocumentProcessor(String path) {
    return new DocumentProcessor(
      path,
      contentReaders,
      preprocessor,
      transformer,
      tokenFilter,
      metaReaders,
      pipeline
    );
  }

}
