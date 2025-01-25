package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.ComparisonPipeline;
import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentAnnotator;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.PairsBuilder;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.*;
import dev.taras.hamkalo.plagdet.io.reporting.ZipAnalysisReportWriter;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {
    final String templatePath = "/home/taras-hamkalo/repositories/plag-det-migration/app/src/main/resources/template/ipc.txt";
//    final String zipPath = "/home/taras-hamkalo/other/ipc/full/21-22";
//    final String zipPath = "/tmp/tests/files";
//    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/test_zipped";
    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/semantic";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/verbatim";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/copy";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/shorts/copy";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/shorts/code-descriptions";
    List<ContentReader> contentReaders = List.of(new DocxDocumentReader(), new DocDocumentReader(), new TextReader());
    List<MetaReader> metaReaders = List.of(new DocxDocumentReader(), new DocDocumentReader(), new TextReader());
    SubmissionsParser submissionsParser = new SubmissionsParser(contentReaders, metaReaders);
    DocumentAnnotator annotator = new DocumentAnnotator(Pipeline.getPipeline());
    PairsBuilder pairsBuilder = new PairsBuilder(
      contentReaders,
      metaReaders,
      annotator,
      new LuceneIndex(null)
    );

//    var index = new LuceneIndex(null);
//    System.out.println(index.inverseTermFrequency(""));

    List<Submission> submissions = submissionsParser.parseSubmissions(zipPath);
    var pairs = compare(submissionsParser, pairsBuilder, submissions, annotator, templatePath);
    ZipAnalysisReportWriter zipAnalysisReportWriter = new ZipAnalysisReportWriter();
    zipAnalysisReportWriter.writeReport(
      submissions, pairs, pairsBuilder.getRetrievedIndexedDocuments()
    );

  }

  private static List<DocumentPair> compare(SubmissionsParser submissionsParser,
                                            PairsBuilder pairsBuilder,
                                            List<Submission> submissions,
                                            DocumentAnnotator annotator,
                                            String templatePath) throws IOException {
    ComparisonPipeline comparison = new ComparisonPipeline(
      new MetadataComparator(),
      new ShingleComparator(0.8, 0.1, 2, 1),
      new SentenceComparator(new LuceneIndex(null))
    );

    var pairs = pairsBuilder.build(submissions);
    compareSubmissionsToTemplate(submissions, submissionsParser, annotator, templatePath);

    var sentenceComparator = new SentenceComparator(new LuceneIndex(null));
    for (var pair : pairs) {
      sentenceComparator.compare(pair);
      log.info("\nPair score {}\nFirst doc {}\nSecond doc {}", pair.getFirst().getFileData(), pair.getSecond().getFileData(), pair.getPlagScores());
    }

    return pairs;
  }

  public static void compareSubmissionsToTemplate(List<Submission> submissions,
                                                  SubmissionsParser submissionsParser,
                                                  DocumentAnnotator annotator,
                                                  String templatePath) throws IOException {
    var templateDocument = submissionsParser.readTemplate(templatePath);
    annotator.annotateDocument(templateDocument);
    var sentenceComparator = new SentenceComparator(new LuceneIndex(null));
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        sentenceComparator.compareWithTemplate(document, templateDocument);
      }
    }
  }

  private static void pushToIndex(SubmissionsParser submissionsParser,
                                  String zipPath,
                                  DocumentAnnotator annotator) throws IOException {
    var submissions = submissionsParser.parseSubmissions(zipPath);
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
