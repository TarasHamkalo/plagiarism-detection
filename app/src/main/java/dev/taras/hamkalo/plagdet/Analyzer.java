package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.analyzers.LuceneAnalyzer;
import dev.taras.hamkalo.plagdet.analyzers.MetaAnalyzer;
import dev.taras.hamkalo.plagdet.comparison.JPlagComparator;
import dev.taras.hamkalo.plagdet.comparison.ngram.FullFingerprintingComparator;
import dev.taras.hamkalo.plagdet.comparison.ngram.Ngram;
import dev.taras.hamkalo.plagdet.comparison.ngram.Ngrams;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.FileSystemStore;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import demos.WordAnnotatorDemo;
import dev.taras.hamkalo.plagdet.util.Utils;
import edu.stanford.nlp.ling.Word;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// TODO: normal implementation
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  DocumentProcessor documentProcessor;

  FileSystemStore fileSystemStore;

  @NonFinal
  MetadataIndex metadataIndex;

  @NonFinal
  LuceneIndex luceneIndex;

  @NonFinal
  MetaAnalyzer metaAnalyzer;

  @NonFinal
  LuceneAnalyzer luceneAnalyzer;

  public void compare(String path1, String path2) {
    var document1 = documentProcessor.readDocument(path1);
    documentProcessor.transform(document1);

    var document2 = documentProcessor.readDocument(path2);
    documentProcessor.transform(document2);

    var comparator = new FullFingerprintingComparator();
    comparator.compare(document1, document2);
//    WordAnnotatorDemo wordAnnotatorDemo = new WordAnnotatorDemo();
//    ArrayList<String> words = new ArrayList<>(document1.getTokens().stream().map(t -> t.getContent()).toList());
//    Ngrams.ngrams(words, 5).forEach(System.out::println);

//    Ngrams.skipgrams(words, 3, 2, Ngrams.SORT_NGRAMS).forEach(System.out::println);

//    wordAnnotatorDemo.createAnnotatedWordDocument(path1, Utils.getFileName(Path.of(path1)) + "_annotated", );
  }

  public void findSimilarTo(String path) {

    var target = documentProcessor.readDocument(path);
    documentProcessor.transform(target);

    // TODO: filter common by path
    var metaSimilarDocuments = metaAnalyzer.analyze(target);
    var contentSimilarDocuments = luceneAnalyzer.analyze(target);
    metaSimilarDocuments.addAll(contentSimilarDocuments);

    JPlagComparator jplag = new JPlagComparator();
    jplag.compare(target, metaSimilarDocuments);
  }

  public String addToIndex(List<String> paths) {
    List<DocumentEntry> documentEntries = new ArrayList<>();

    for (String path : paths) {
      var documentEntry = documentProcessor.readDocument(path);
      documentProcessor.transform(documentEntry);

      documentEntry = fileSystemStore.addToStore(documentEntry);

      documentEntries.add(documentEntry);

      metadataIndex.pushToIndex(documentEntry);
      luceneIndex.pushToIndex(documentEntry);
    }

    return "added to index " + documentEntries.size();
  }


}
