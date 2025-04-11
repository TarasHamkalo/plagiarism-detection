package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.*;
import dev.taras.hamkalo.plagdet.comparision.blocks.IndexBasedWordAlignmentSeedingStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.NopExtensionStrategy;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.documents.corpus.BaseCorpus;
import dev.taras.hamkalo.plagdet.documents.corpus.Corpus;
import dev.taras.hamkalo.plagdet.documents.pool.BaseDocumentPool;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.filedata.EagerFileDataSource;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.io.filedata.reader.*;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @NotNull PlagdetParameters parameters;

  @NotNull List<FileDataReader> registeredFileDataReaders;

  public Plagdet(@NotNull PlagdetParameters parameters) {
    this.parameters = parameters;
    this.registeredFileDataReaders = List.of(
      new DocxFileDataReader(), new PdfFileDataReader(),
      new DocFileDataReader(), new TextFileDataReader()
    );

    this.luceneIndex = initLucene(parameters.getLuceneIndexDirectoryPath());
    this.embeddings = new Embeddings(luceneIndex);

    ExecutorServiceSingleton.setNumberOfProcessors(parameters.getNumberOfProcessors());
  }

  private @NotNull LuceneIndex initLucene(@Nullable String luceneIndexDirectoryPath) {
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

  public void compare(@NotNull String path) {
    Pipeline.init();
    FileDataSource fileDataSource = new EagerFileDataSource(registeredFileDataReaders, path);
    Corpus corpus = new BaseCorpus(fileDataSource);
    DocumentPool documentPool = new BaseDocumentPool(corpus, embeddings);
    List<ComparisonFilter> filters = List.of(
      new LengthComparisonFilter(parameters.getLengthComparisonFilterParameters()),
      new ShingleOverlapComparisonFilter(parameters.getShingleOverlapComparisonFilterParameters())
    );
    Comparator comparator = new Comparator(
      new NopExtensionStrategy(),
      new IndexBasedWordAlignmentSeedingStrategy(embeddings),
      new MetadataComparator(parameters.getMetadataComparatorParameters()),
      filters
    );
    int i = 0;
    int comparisonsCount = 0;
    while (documentPool.hasAnySuspiciousDocument()) {
      log.trace("Source document index [{}]\n\n", i);
      var susp = documentPool.nextSuspiciousDocument();
      System.gc();
      documentPool.prepareSuspiciousDocument();
      while (documentPool.hasAnySourceDocument()) {
        var src = documentPool.nextSourceDocument();
        var comparison = comparator.comparePair(documentPool);
//        log.trace("Comparing:\n\t1: {}\n\t2: {}", susp.getFileData(), src.getFileData());
        log.trace("Comparison {}:\n\t {}\n", comparisonsCount++, comparison);
      }

      i++;
    }

  }


//  public void index(@NotNull List<DocumentEntry> documentEntries) {
//    for (var document : documentEntries) {
//      luceneIndex.addToIndex(document);
//    }
//  }

//  public void index(@NotNull String path) {
//    Pipeline.init();
//    ExecutorServiceSingleton.getExecutorService().init();
//
//    ExecutorServiceSingleton.getExecutorService().deInit();
//  }
}

