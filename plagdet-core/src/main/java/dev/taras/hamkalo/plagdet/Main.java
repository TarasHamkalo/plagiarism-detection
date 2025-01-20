package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentAnnotator;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.DocDocumentReader;
import dev.taras.hamkalo.plagdet.io.reader.DocxDocumentReader;
import dev.taras.hamkalo.plagdet.io.reader.TextReader;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {

    final String zipPath = "/home/taras-hamkalo/other/ipc/full/21-22";
//    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/test_zipped";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new DocxDocumentReader(), new DocDocumentReader(), new TextReader()),
      List.of(new DocxDocumentReader(), new DocDocumentReader())
    );

    DocumentAnnotator annotator = new DocumentAnnotator(Pipeline.getPipeline());

    final var submissions = submissionsParser.parseSubmissions(zipPath);

    annotator.annotateSubmissions(submissions);
    LuceneIndex luceneIndex = new LuceneIndex(null);

    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        log.info("Adding {}", document.getFileData().getPath().toString());
        luceneIndex.addToIndex(document);
      }
      break;
    }
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        var results = luceneIndex.retrieveSimilar(document, 10);
        System.out.println(results);
      }
      break;
    }
  }

}
