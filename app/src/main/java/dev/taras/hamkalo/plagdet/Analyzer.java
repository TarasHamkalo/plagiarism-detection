package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.DocumentIndex;
import dev.taras.hamkalo.plagdet.index.FileSystemIndex;
import dev.taras.hamkalo.plagdet.index.LuceneIndex;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.filter.TokenFilter;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.Transformer;
import dev.taras.hamkalo.plagdet.readers.MetaReader;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

// TODO: normal implementation
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  List<DocumentProcessor> documentProcessors = new ArrayList<>();

  ContentReader contentReader;

  Preprocessor preprocessor;

  Transformer transformer;

  TokenFilter tokenFilter;

  MetaReader metaReader;

  public void findSimilarTo(String path) {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    DocumentIndex documentIndex = new LuceneIndex();
    var docProc = new DocumentProcessor(
      path,
      contentReader,
      preprocessor,
      transformer,
      tokenFilter,
      metaReader,
      pipeline
    );

    docProc.preprocess();
    var docEntry = docProc.transform();

    documentIndex.retrieveSimilarByContent(docEntry).forEach(System.out::println);

  }

  public String analyze(List<String> paths) {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize");
//    props.setProperty("tokenize.options", "splitHyphenated=true");
//    props.setProperty("tokenize.whitespace", "false"); // This ensures diacritics are not stripped as part of whitespace handling
//    props.setProperty("tokenize.options", "normalizeParentheses=false,normalizeOtherBrackets=false");

    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    DocumentIndex luceneIndex = new LuceneIndex();
    DocumentIndex fsIndex = new FileSystemIndex();

    List<DocumentProcessor> documentProcessors = new ArrayList<>();
    for (String path : paths) {
      documentProcessors.add(
        new DocumentProcessor(
          path,
          contentReader,
          preprocessor,
          transformer,
          tokenFilter,
          metaReader,
          pipeline
        )
      );
    }

    List<DocumentEntry> documentEntries = new ArrayList<>();
    for (var documentProcessor : documentProcessors) {
      var documentEntry = documentProcessor.preprocess();
      documentEntry = documentProcessor.transform();
      luceneIndex.pushToIndex(documentEntry);
      documentEntries.add(documentEntry);
      fsIndex.pushToIndex(documentEntry);
    }

    luceneIndex.retrieveSimilarByContent(documentEntries.get(0)).forEach(System.out::println);
    return "analysis done";
  }

}
