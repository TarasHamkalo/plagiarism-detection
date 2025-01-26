package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import dev.taras.hamkalo.plagdet.io.reporting.ZipAnalysisReportWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {
    final String templatePath = "/home/taras-hamkalo/repositories/plag-det-migration/app/src/main/resources/template/ipc.txt";
//    final String zipPath = "/home/taras-hamkalo/other/ipc/full/21-22";
    final String zipPath = "/home/taras-hamkalo/other/ipc/full/20-21";
//    final String zipPath = "/tmp/tests/files";
//    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/test_zipped";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/semantic";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/verbatim";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/copy";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/shorts/copy";
//    final String zipPath = "/home/taras-hamkalo/repositories/plag-det-migration/plagdet-core/src/main/resources/files/shorts/code-descriptions";

    var parameters = PlagdetParameters.builder()
      .similarDocumentSourceParameters(SimilarDocumentSource.SimilarDocumentSourceParameters.builder().build())
      .sentenceComparatorParameters(SentenceComparator.SentenceComparatorParameters.builder().build())
      .shingleComparatorParameters(ShingleComparator.ShingleComparatorParameters.builder().build())
      .metadataComparatorParameters(MetadataComparator.MetadataComparatorParameters.builder().build())
      .luceneIndexDirectoryPath("lucene_index")
      .templateDocumentPath(templatePath)
      .build();

    log.warn("Parameters are\n{}\n\n", parameters);
    Plagdet plagdet = new Plagdet(parameters);

    var report = plagdet.compare(zipPath);
//    plagdet.index(zipPath);
    ZipAnalysisReportWriter zipAnalysisReportWriter = new ZipAnalysisReportWriter();
    zipAnalysisReportWriter.writeReport(report);

  }



}
