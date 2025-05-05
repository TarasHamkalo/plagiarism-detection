package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy.IndexBasedWordAlignmentSeedingStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy.LengthBasedFilteringStrategyParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter.LengthComparisonFilterParameters;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneFileDataSource.LuceneFileDataSourceParameters;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static dev.taras.hamkalo.plagdet.ComparisonsStore.ComparisonsStoreParameters;
import static dev.taras.hamkalo.plagdet.comparision.MetadataComparator.MetadataComparatorParameters;
import static dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy.*;
import static dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters;
import static dev.taras.hamkalo.plagdet.preprocessing.Pipeline.*;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {
//    PlagdetCli.run(args);
    test();
  }

  public static void test() {
    final String templatePath = "/home/taras-hamkalo/repositories/plagdet-v3/demos/src/main/resources/template/ipc.txt";
//    final String inputPath = "/home/taras-hamkalo/other/ipc/full/13-14";
//    final String inputPath = "/home/taras-hamkalo/other/ipc/full/merge-13-23";
//    final String inputPath = "/home/taras-hamkalo/other/ipc/full/23-24";
//    final String inputPath = "/home/taras-hamkalo/other/ipc/full/subset/50";
//    final String inputPath = "/home/taras-hamkalo/repositories/plagdet-v3/test_files/cases/15";
    final String inputPath = "/home/taras-hamkalo/other/corpuses/test-dataset/test-dataset.zip";

    final String luceneIndex = "/home/taras-hamkalo/other/ipc/indexes/lucene-main";
//    final String luceneIndex = "/tmp/lucene_index_2";
    var parameters = PlagdetParameters.builder()
      .reportFileName("durka")
      .filteringStrategyParameters(LengthBasedFilteringStrategyParameters.builder().minCharacterLength(70).build())
      .extensionStrategyParameters(RecursiveClusteringExtensionStrategyParameters.builder().minClusterSize(4).sentMaxGap(2).build())
      .pipelineParameters(PipelineParameters.builder().canBypassTemplateRemoval(false).build())
      .luceneFileDataSourceParameters(LuceneFileDataSourceParameters.builder().similarDocumentsLimit(0).maxMetadataSimilarDoc(0).build())
      .comparisonsStoreParameters(ComparisonsStoreParameters.builder().canBypassFiltering(true).maxResultsPerDocument(70).minSimilarityToRecord(0.4).build())
      .metadataComparatorParameters(MetadataComparatorParameters.builder().canBypassFiltering(true).build())
      .shingleOverlapComparisonFilterParameters(ShingleOverlapComparisonFilterParameters.builder().canBypassFiltering(true).build())
      .lengthComparisonFilterParameters(LengthComparisonFilterParameters.builder().build())
      .luceneIndexDirectoryPath(luceneIndex)
      .seedingStrategyParameters(IndexBasedWordAlignmentSeedingStrategyParameters.builder().build())
      .build();

    log.warn("Parameters are\n{}\n\n", parameters);
    Plagdet plagdet = new Plagdet(parameters);
//    plagdet.findSimilar(inputPath);
//    plagdet.index(inputPath);
    plagdet.compare(inputPath);
//    plagdet.memoryTest(inputPath);
  }
}