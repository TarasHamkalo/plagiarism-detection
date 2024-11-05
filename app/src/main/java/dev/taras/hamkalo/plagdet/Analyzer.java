package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.analyzers.LuceneAnalyzer;
import dev.taras.hamkalo.plagdet.analyzers.MetaAnalyzer;
import dev.taras.hamkalo.plagdet.comparison.JPlagComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.FileSystemStore;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// TODO: normal implementation
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  DocumentProcessor documentProcessor;

  FileSystemStore fileSystemStore;

  MetadataIndex metadataIndex;

  LuceneIndex luceneIndex;

  MetaAnalyzer metaAnalyzer;

  LuceneAnalyzer luceneAnalyzer;

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
//    LuceneIndex luceneIndex = new LuceneIndex();
//    // Only path is returned
//    // Content should be provided
//    luceneIndex.retrieveSimilar(documentEntry);
//    // path and metadata, path to docx or txt (fs index will store orig + txt normalized)
//    // Content should be provided
//    metadataIndex.retrieveSimilar(documentEntry);
//    fillContent(...)
//    indexSequence.retrieveSimilar(documentEntry).forEach(System.out::println);
//    createDir( for JPlag )
//  analyze
//    indexSequence.
