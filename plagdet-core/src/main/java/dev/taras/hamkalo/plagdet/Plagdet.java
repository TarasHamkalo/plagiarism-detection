package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.*;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Plagdet {

  @NotNull Embeddings embeddings;

  @NotNull
  @Getter
  LuceneIndex luceneIndex;

  @NotNull SimilarDocumentSource similarDocumentSource;

  @NotNull SubmissionsParser submissionsParser;

  @Nullable DocumentEntry template;

  @NotNull ComparisonPipeline comparisonPipeline;

  @NotNull PlagdetParameters parameters;

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

  }

  private SubmissionsParser initSubmissionsParser() {
    List<ContentReader> contentReaders = List.of(
      new DocxDocumentReader(), new DocDocumentReader(), new PdfDocumentReader(), new TextReader()
    );
    List<MetadataReader> metadataReaders = List.of(
      new DocxDocumentReader(), new DocDocumentReader(), new PdfDocumentReader(), new TextReader()
    );

    return new SubmissionsParser(contentReaders, metadataReaders);
  }

  private @NotNull ComparisonPipeline initComparisonPipeline(
    @NotNull PlagdetParameters parameters,
    @NotNull Embeddings embeddings,
    @NotNull SimilarDocumentSource similarDocumentSource,
    @Nullable DocumentEntry template
  ) {

    DocumentComparator sentenceComparator = new SentenceComparator(
      parameters.getSentenceComparatorParameters(), embeddings
    );

    return new ComparisonPipeline(
      similarDocumentSource,
      (TemplateComparator) sentenceComparator,
      new ResultsFilter(parameters.getResultsFilterParameters()),
      template,
      new MetadataComparator(parameters.getMetadataComparatorParameters()),
      new ShingleComparator(parameters.getShingleComparatorParameters()),
      sentenceComparator
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
    var submissions = submissionsParser.parseSubmissions(path);

    Pipeline.init();
    ExecutorServiceSingleton.getExecutorService().init();

    var ignore = comparisonPipeline.compareSubmissions(submissions);

    var deduplicatedComparisonResults = submissions.stream()
      .flatMap(s -> s.getDocuments().stream())
      .flatMap(d -> d.getDocumentPairs().stream())
      .collect(Collectors.toMap(
        (p) -> p.getId(),
        p -> p,
        (p1, p2) -> p1)
      );

    SimilarDocumentSource.getRetrievedIndexedDocuments().clear();
    var filteredRetrievedDocuments = deduplicatedComparisonResults.values().stream()
      .map(DocumentPair::getSecond)
      .filter(DocumentEntry::isIndexed)
      .toList();

    ExecutorServiceSingleton.getExecutorService().deInit();
    return Report.builder()
      .inputPath(path)
      .plagdetParameters(this.parameters)
      .submissions(submissions)
      .comparisonsCount(comparisonPipeline.getComparisonsCount())
      .pairs(new ArrayList<>(deduplicatedComparisonResults.values()))
      .documentsRetrievedFromIndex(filteredRetrievedDocuments)
      .build();
  }


  public void index(@NotNull List<Submission> submissions) {
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        luceneIndex.addToIndex(document);
      }
    }
  }

  public void index(@NotNull String path) {
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
