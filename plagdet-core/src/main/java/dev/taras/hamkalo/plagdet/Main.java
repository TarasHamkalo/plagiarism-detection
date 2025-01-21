package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.ComparisonPipeline;
import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentAnnotator;
import dev.taras.hamkalo.plagdet.documents.PairsBuilder;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.*;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {
//    final String zipPath = "/home/taras-hamkalo/other/ipc/full/21-22";
//    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/test_zipped";
    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/semantic";
    List<ContentReader> contentReaders = List.of(new DocxDocumentReader(), new DocDocumentReader(), new TextReader());
    List<MetaReader> metaReaders = List.of(new DocxDocumentReader(), new DocDocumentReader());
//    SubmissionsParser submissionsParser = new SubmissionsParser(contentReaders, metaReaders);
    DocumentAnnotator annotator = new DocumentAnnotator(Pipeline.getPipeline());
    PairsBuilder pairsBuilder = new PairsBuilder(
      contentReaders,
      metaReaders,
      annotator,
      new LuceneIndex(null)
    );

//    var index = new LuceneIndex(null);
//    System.out.println(index.inverseTermFrequency(""));
    compare(pairsBuilder, zipPath);
  }

  private static void compare(PairsBuilder pairsBuilder, String zipPath) throws IOException {
    ComparisonPipeline comparison = new ComparisonPipeline(
      new MetadataComparator(),
      new ShingleComparator(0.8, 0.1, 2, 1)
    );

    SentenceComparator sentenceComparator = new SentenceComparator(
      new LuceneIndex(null)
    );

    var pairs = pairsBuilder.build(zipPath);
    sentenceComparator.compare(pairs.get(0));
//    comparison.compare(pairs);
//    for (var pair : pairs) {

//      System.out.println(pair);
//    }
  }

  private static void pushToIndex(SubmissionsParser submissionsParser, String zipPath, DocumentAnnotator annotator) throws IOException {
    var submissions = submissionsParser.parseSubmissions(zipPath);
//    var pairs = pairsBuilder.build(zipPath);
    annotator.annotateSubmissions(submissions);
    LuceneIndex luceneIndex = new LuceneIndex(null);
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        log.debug("Adding document: {}", document.getFileData());
        luceneIndex.addToIndex(document);
      }
    }
  }

}
