package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.ComparisonResult;
import dev.taras.hamkalo.plagdet.comparision.comparator.DocumentComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.TemplateComparator;
import dev.taras.hamkalo.plagdet.concurrency.ExecutorServiceSingleton;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComparisonPipeline {

  @NotNull List<DocumentComparator> comparators;

  @NotNull SimilarDocumentSource similarDocumentSource;

  @NotNull Queue<DocumentPair> comparisonTasks = new LinkedList<>();

  @NotNull Queue<DocumentPair> comparisonResults = new ConcurrentLinkedQueue<>();

  @NotNull ResultsFilter resultsFilter;

  @NotNull TemplateComparator templateComparator;

  @Nullable DocumentEntry template;

  @Getter
  @NonFinal
  int comparisonsCount;

  public ComparisonPipeline(
    @NotNull SimilarDocumentSource similarDocumentSource,
    @NotNull TemplateComparator templateComparator,
    @NotNull ResultsFilter resultsFilter,
    @Nullable DocumentEntry template,
    @NotNull DocumentComparator... documentComparators
  ) {
    this(
      new ArrayList<>(Arrays.asList(documentComparators)),
      similarDocumentSource,
      resultsFilter,
      templateComparator,
      template
    );
  }

  /**
   * @param submissions parsed, not annotated submissions
   * @return list of compared document pair
   */
  public @NotNull List<DocumentPair> compareSubmissions(@NotNull List<Submission> submissions) {
    Pipeline.init();
    ExecutorServiceSingleton.getExecutorService().init();

    final var documents = submissions.stream()
      .flatMap(s -> s.getDocuments().stream())
      .toList();

    preprocessTemplate();

    preprocessDocuments(documents);

    prepareComparisonTasks(submissions);

    preprocessDocuments(SimilarDocumentSource.getRetrievedIndexedDocuments().values());

    doCompare();

    ExecutorServiceSingleton.getExecutorService().deInit();
    return new ArrayList<>(this.comparisonResults);
  }

  private void preprocessDocuments(@NotNull Collection<DocumentEntry> documentEntries) {
    try {
      ExecutorServiceSingleton.getExecutorService().invokeAll(
        documentEntries.stream()
          .map(this::toPreprocessingTask)
          .toList()
      );
    } catch (InterruptedException e) {
      throw new PlagdetException("Was not able to preprocess all documents");
    }

  }

  private void preprocessTemplate() {
    if (template == null) {
      log.info("Template file was not provided, skipping");
      return;
    }

    if (!template.isAnnotated()) {
      CoreDocument coreDocument = new CoreDocument(template.getFileData().getContent());
      Pipeline.annotate(coreDocument);
      template.setCoreDocument(coreDocument);
    }
  }

  private @NotNull Callable<Void> toPreprocessingTask(@NotNull DocumentEntry d) {
    return () -> {
      CoreDocument coreDocument = new CoreDocument(d.getFileData().getContent());
      Pipeline.annotate(coreDocument);
      d.setCoreDocument(coreDocument);
      if (template != null) {
        templateComparator.compareWithTemplate(d, template);
      }
      return null;
    };
  }

  private void doCompare() {

    log.info("Comparisons count is {}", comparisonTasks.size());

    List<Future<DocumentPair>> futures = new ArrayList<>();
    var executor = ExecutorServiceSingleton.getExecutorService();
    while (!comparisonTasks.isEmpty()) {
      var comparisonTask = comparisonTasks.poll();
      futures.add(executor.submit(() -> {
        for (DocumentComparator comparator : comparators) {
          ComparisonResult result = comparator.compare(comparisonTask);
          if (!ComparisonResult.CONTINUE.equals(result)) {
            break;
          }
        }

        log.info("Comparing pair:\n\tFirst document {}\n\tSecond document {}\n\tPair scores {}\n",
          comparisonTask.getFirst().getFileData().getPersistentPath(),
          comparisonTask.getSecond().getFileData().getPersistentPath(),
          comparisonTask.getPlagScores()
        );

        return comparisonTask;
      }));
    }

    for (Future<DocumentPair> future : futures) {
      try {
        addComparisonResults(future.get());
      } catch (InterruptedException | ExecutionException e) {
        log.error("Comparison task failed", e);
      }
    }
  }

  private void addComparisonResults(@NotNull DocumentPair comparisonTask) {
    if (resultsFilter.filterDocumentPair(comparisonTask)) {
      resultsFilter.safeAddDocumentPair(comparisonTask.getFirst(), comparisonTask);
      resultsFilter.safeAddDocumentPair(comparisonTask.getSecond(), comparisonTask);
      comparisonResults.offer(comparisonTask);
    }
  }

  private void prepareComparisonTasks(@NotNull List<Submission> submissions) {
    for (int i = 0; i < submissions.size(); i++) {
      Submission first = submissions.get(i);
      for (var firstDocument : first.getDocuments()) {
        if (firstDocument.isHasPassedLuceneSearch()) {
          continue;
        }

        var similarDocuments = similarDocumentSource.retrieveSimilar(firstDocument);
        addComparisonTasks(firstDocument, similarDocuments);
        for (int j = i + 1; j < submissions.size(); j++) {
          Submission second = submissions.get(j);
          addComparisonTasks(firstDocument, second.getDocuments());
        }

        firstDocument.setHasPassedLuceneSearch(true); // kinda okay, when interrupt will occur, comparison tasks will
      }
    }
    this.comparisonsCount = comparisonTasks.size();
  }

  private void addComparisonTasks(
    @NotNull DocumentEntry suspicious,
    @NotNull List<DocumentEntry> sources
  ) {

    sources.stream()
      .map(source ->
        DocumentPair.builder()
          .first(suspicious)
          .second(source)
          .build()
      )
      .forEach(comparisonTasks::add);
  }

  public ComparisonPipeline addComparator(DocumentComparator comparator) {
    this.comparators.add(comparator);
    return this;
  }

}
