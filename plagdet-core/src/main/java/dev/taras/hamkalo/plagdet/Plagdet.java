package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.Comparator;
import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.LengthBasedFilteringStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.RecursiveClusteringExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.filter.ComparisonFilter;
import dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter;
import dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.documents.corpus.*;
import dev.taras.hamkalo.plagdet.documents.pool.BaseDocumentPool;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneFileDataSource;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.io.filedata.LazyFileDataSource;
import dev.taras.hamkalo.plagdet.io.filedata.reader.*;
import dev.taras.hamkalo.plagdet.io.report.BufferedReportWriter;
import dev.taras.hamkalo.plagdet.io.report.model.OverviewJsonModel;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LearningPolicy;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Plagdet {

  @NotNull Embeddings embeddings;

  @NotNull LuceneIndex luceneIndex;

  @NotNull Comparator comparator;

  @NotNull PlagdetParameters parameters;

  @NotNull List<FileDataReader> registeredFileDataReaders;

  @NotNull WorkspaceConfiguration indexingMemoryWorkspace = WorkspaceConfiguration.builder()
    .initialSize(2 * 1024L * 1024L)
    .maxSize(5 * 1024L * 1024L)
    .policyAllocation(AllocationPolicy.STRICT)
    .policyLearning(LearningPolicy.NONE)
    .build();

  public Plagdet(@NotNull PlagdetParameters parameters) {
    this.parameters = parameters;
    this.registeredFileDataReaders = List.of(
      new DocxFileDataReader(), new PdfFileDataReader(),
      new DocFileDataReader(), new TextFileDataReader()
    );

    this.luceneIndex = createLucene(parameters.getLuceneIndexDirectoryPath());
    this.embeddings = new Embeddings(luceneIndex);
    this.comparator = createComparator(embeddings, parameters);
  }

  private @NotNull LuceneIndex createLucene(@Nullable String luceneIndexDirectoryPath) {
    try {
      if (luceneIndexDirectoryPath == null) {
        Path luceneTempDir = Files.createTempDirectory("temporary_lucene_index_");
        return new LuceneIndex(luceneTempDir);
      }

      return new LuceneIndex(Path.of(luceneIndexDirectoryPath));
    } catch (IOException e) {
      throw new PlagdetException(
        String.format("Failed to init lucene with directory %s", luceneIndexDirectoryPath), e
      );
    }
  }

  private @NotNull Comparator createComparator(
    @NotNull Embeddings embeddings,
    @NotNull PlagdetParameters parameters
  ) {
    List<ComparisonFilter> filters = List.of(
      new MetadataComparator(parameters.getMetadataComparatorParameters()),
      new LengthComparisonFilter(parameters.getLengthComparisonFilterParameters()),
      new ShingleOverlapComparisonFilter(parameters.getShingleOverlapComparisonFilterParameters())
    );

    return new Comparator(
//      new NopExtensionStrategy(),
      new LengthBasedFilteringStrategy(parameters.getFilteringStrategyParameters()),
      new RecursiveClusteringExtensionStrategy(embeddings, parameters.getExtensionStrategyParameters()),
      new IndexBasedWordAlignmentSeedingStrategy(embeddings, parameters.getSeedingStrategyParameters()),
      new MetadataComparator(parameters.getMetadataComparatorParameters()),
      filters
    );
  }

  private void initPipeline() {
    String templateContent = null;
    if (parameters.getTemplateDocumentPath() != null) {
      templateContent = IoUtils.getFileContent(
        registeredFileDataReaders,
        parameters.getTemplateDocumentPath()
      );
    }

    Pipeline.init(parameters.getPipelineParameters(), embeddings, templateContent);
  }

  // TODO: wrap to PlagDet exception
  @SneakyThrows
  public void compare(@NotNull String path) {
    initPipeline();

    ComparisonsStore comparisonsStore =
      new ComparisonsStore(parameters.getComparisonsStoreParameters());
    BufferedReportWriter bufferedReportWriter = new BufferedReportWriter(comparisonsStore);
    FileDataSource fileDataSource = new LazyFileDataSource(registeredFileDataReaders, path);
    Corpus corpus = new ContentRemovalCorpus(bufferedReportWriter, fileDataSource);
    DocumentPool documentPool = new BaseDocumentPool(corpus, embeddings);

    int i = 0;
    int comparisonsCount = 0;
    int estimateFilesCount = fileDataSource.estimateFilesCount();
    log.info("Starting comparison, files begin to load into memory, first iteration could be slower");
    while (documentPool.hasAnySuspiciousDocument()) {
      var susp = documentPool.nextSuspiciousDocument();
      log.info(
        "Processing file [{}/{}] with path [{}]",
        i + 1, estimateFilesCount,
        susp.getFileData().getPersistentPath()
      );
      comparisonsCount += compareSingle(documentPool, bufferedReportWriter, comparisonsCount);
      i++;
    }

    bufferedReportWriter.createReport(
      OverviewJsonModel.builder()
        .inputPath(path)
        .plagdetParameters(parameters)
        .build()
    );

    luceneIndex.close();
  }

  private int compareSingle(
    @NotNull DocumentPool documentPool,
    @NotNull BufferedReportWriter bufferedReportWriter,
    int comparisonsCount
  ) {
    var susp = documentPool.getSuspiciousDocument();
    documentPool.prepareSuspiciousDocument();
    while (documentPool.hasAnySourceDocument()) {
      documentPool.nextSourceDocument();
      var comparison = comparator.compare(documentPool);
      log.trace("Comparisons done [{}]", comparisonsCount++);
      bufferedReportWriter.writeComparison(comparison, false);
    }

    FileDataSource luceneSource = new LuceneFileDataSource(
      luceneIndex, susp, parameters.getLuceneFileDataSourceParameters()
    );

    IndexableCorpus indexedCorpus = new LuceneIndexableCorpus(
      embeddings,
      luceneSource,
      AnnotationUtils.sentences(susp.getAnnotation()).size(),
      parameters.getLengthComparisonFilterParameters().getSentenceSizeRatioThreshold()
    );

    log.info("Starting comparison to indexed documents");
    var indexComparisons = comparator.compare(documentPool, indexedCorpus);
    log.info("Comparison to indexed documents done");
    indexComparisons.forEach(c -> bufferedReportWriter.writeComparison(c, true));
    bufferedReportWriter.flushDocumentMetadata(susp, indexedCorpus);
    indexedCorpus.clear();
    indexedCorpus.close();
    embeddings.clearSentenceVectorCache();
    return comparisonsCount;
  }

  public void index(@NotNull String path) {
    initPipeline();
    ExecutorServiceSingleton.getExecutorService().init();
    FileDataSource fileDataSource = new LazyFileDataSource(registeredFileDataReaders, path);
    Corpus corpus = new BaseCorpus(fileDataSource);

    Nd4j.getMemoryManager().togglePeriodicGc(false);

    int i = 0;
    int estimateFilesCount = fileDataSource.estimateFilesCount();
    while (corpus.hasNext()) {
      var ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(
        indexingMemoryWorkspace, "PD_INDEXING"
      );

      try (ws) {
        var document = corpus.pop();
        log.info(
          "[{}/{}] Indexing [{}]",
          i + 1, estimateFilesCount,
          document.getFileData().getPersistentPath()
        );
        luceneIndex.addToIndex(document);
        i++;
      }
      embeddings.clearSentenceVectorCache();
    }

    luceneIndex.close();
    ExecutorServiceSingleton.getExecutorService().deInit();
  }
}

