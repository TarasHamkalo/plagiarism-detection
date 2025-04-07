package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.*;
import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.*;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Plagdet {

  @NotNull Embeddings embeddings;

  @NotNull LuceneIndex luceneIndex;

  @NotNull SimilarDocumentSource similarDocumentSource;

  @NotNull SubmissionsParser submissionsParser;

  @Nullable DocumentEntry template;

  @NotNull ComparisonPipeline comparisonPipeline;

  @NotNull PlagdetParameters parameters;

  @NotNull
  @NonFinal
  List<Submission> submissions = new ArrayList<>();

  @NonFinal
  @Nullable String currentInputPath;

  public Plagdet(@NotNull PlagdetParameters parameters) {
    this.parameters = parameters;
    this.luceneIndex = this.initLucene(parameters.getLuceneIndexDirectoryPath());
    this.similarDocumentSource = new SimilarDocumentSource(
      parameters.getSimilarDocumentSourceParameters(), this.luceneIndex
    );

    this.submissionsParser = this.initSubmissionsParser();
    this.template = submissionsParser.readTemplate(parameters.getTemplateDocumentPath());
    this.embeddings = new Embeddings(luceneIndex);
    this.comparisonPipeline = this.initComparisonPipeline(
      parameters, embeddings, similarDocumentSource, template
    );

    ExecutorServiceSingleton.setNumberOfProcessors(parameters.getNumberOfProcessors());
  }

  private SubmissionsParser initSubmissionsParser() {
    return new SubmissionsParser(List.of(
      new DocxDocumentReader(), new DocDocumentReader(),
      new PdfDocumentReader(), new TextDocumentReader()
    ));
  }

  private @NotNull ComparisonPipeline initComparisonPipeline(
    @NotNull PlagdetParameters parameters,
    @NotNull Embeddings embeddings,
    @NotNull SimilarDocumentSource similarDocumentSource,
    @Nullable DocumentEntry template
  ) {
    return new ComparisonPipeline(
      similarDocumentSource,
      new SentenceComparator(parameters.getTemplateComparatorParameters(), embeddings),
      new ResultsFilter(parameters.getResultsFilterParameters()),
      template,
      new MetadataComparator(parameters.getMetadataComparatorParameters()),
      new ShingleComparator(parameters.getShingleComparatorParameters()),
      new SentenceComparator(parameters.getSentenceComparatorParameters(), embeddings)
    );
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

  public Report compare(@NotNull String path) {
    currentInputPath = path;
    System.gc();
    submissions = submissionsParser.parseSubmissions(path);
    System.out.println("Parsed with uptime " + ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    Pipeline.init();
    ExecutorServiceSingleton.getExecutorService().init();
    comparisonPipeline.compareSubmissions(submissions);
    ExecutorServiceSingleton.getExecutorService().deInit();
    return Report.from(this, false);
  }


  public void index(@NotNull List<Submission> submissions) {
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        luceneIndex.addToIndex(document);
      }
    }
  }

  public void index(@NotNull String path) {
    Pipeline.init();
    ExecutorServiceSingleton.getExecutorService().init();
    final var submissions = submissionsParser.parseSubmissions(path);
    final var documents = submissions.stream()
      .flatMap(s -> s.getDocuments().stream())
      .toList();

    comparisonPipeline.preprocessDocuments(documents);
    index(submissions);

    ExecutorServiceSingleton.getExecutorService().deInit();
  }

}
