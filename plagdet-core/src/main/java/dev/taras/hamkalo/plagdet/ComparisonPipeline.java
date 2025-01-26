package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.ComparisonResult;
import dev.taras.hamkalo.plagdet.comparision.comparator.DocumentComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.TemplateComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentAnnotator;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComparisonPipeline {

  @NotNull List<DocumentComparator> comparators;

  @NotNull DocumentAnnotator documentAnnotator;

  @NotNull SimilarDocumentSource similarDocumentSource;

  @NotNull Queue<DocumentPair> comparisonTasks = new LinkedList<>();

  @NotNull PriorityQueue<DocumentPair> comparisonResults = new PriorityQueue<>();

  @NotNull TemplateComparator templateComparator;

  @Nullable DocumentEntry template;

  public ComparisonPipeline(
    @NotNull DocumentAnnotator documentAnnotator,
    @NotNull SimilarDocumentSource similarDocumentSource,
    @NotNull TemplateComparator templateComparator,
    @NotNull DocumentEntry template,
    @NotNull DocumentComparator... documentComparators
  ) {

    this(
      new ArrayList<>(Arrays.asList(documentComparators)),
      documentAnnotator,
      similarDocumentSource,
      templateComparator,
      template
    );
  }

  /**
   * @param submissions parsed, not annotated submissions
   * @return list of compared document pair
   */
  public @NotNull List<DocumentPair> compareSubmissions(@NotNull List<Submission> submissions) {
    documentAnnotator.annotateSubmissions(submissions);
    compareWithTemplate(submissions);
    prepareComparisonTasks(submissions);
    doCompare();
    return new ArrayList<>(this.comparisonResults);
  }

  private void compareWithTemplate(@NotNull List<Submission> submissions) {
    if (template == null) {
      log.info("Template document was not provided, skipping");
      return;
    }

    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        templateComparator.compareWithTemplate(document, template);
      }
    }

  }

  private void doCompare() {
    for (int i = 0; i < comparisonTasks.size(); i++) {
      var comparisonTask = this.comparisonTasks.poll();
      for (DocumentComparator comparator : comparators) {
        ComparisonResult result = comparator.compare(comparisonTask);
        if (!ComparisonResult.CONTINUE.equals(result)) {
          break;
        }
      }
      this.comparisonResults.add(comparisonTask);
    }
  }

  private void prepareComparisonTasks(@NotNull List<Submission> submissions) {
    for (int i = 0; i < submissions.size(); i++) {
      Submission first = submissions.get(i);
      for (var firstDocument : first.getDocuments()) {
        var similarDocuments = similarDocumentSource.retrieveSimilar(firstDocument);
        documentAnnotator.annotateDocuments(similarDocuments);
        firstDocument.getDocumentPairs().addAll(
          addComparisonTasks(firstDocument, similarDocuments)
        );

        for (int j = i + 1; j < submissions.size(); j++) {
          Submission second = submissions.get(j);
          var submissionToSubmissionTasks = addComparisonTasks(firstDocument, second.getDocuments());
          firstDocument.getDocumentPairs().addAll(submissionToSubmissionTasks);

          for (int k = 0; k < second.getDocuments().size(); k++) {
            second.getDocuments().get(k).getDocumentPairs().add(submissionToSubmissionTasks.get(k));
          }
        }

      }
    }
  }

  private @NotNull List<DocumentPair> addComparisonTasks(
    @NotNull DocumentEntry suspicious,
    @NotNull List<DocumentEntry> sources
  ) {

    var newComparisonTask = sources.stream()
      .map(source ->
        DocumentPair.builder()
          .first(suspicious)
          .second(source)
          .build()
      )
      .collect(Collectors.toList());

    this.comparisonTasks.addAll(newComparisonTask);
    return newComparisonTask;
  }

  public ComparisonPipeline addComparator(DocumentComparator comparator) {
    this.comparators.add(comparator);
    return this;
  }

}
