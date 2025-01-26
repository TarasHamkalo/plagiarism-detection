package dev.taras.hamkalo.plagdet.indexing;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneResult;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.io.Metadata;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SimilarDocumentSource {

  @Value
  @Builder
  @AllArgsConstructor
  public static class SimilarDocumentSourceParameters {

    public static final int DEFAULT_SIMILAR_DOCUMENTS_LIMIT = 10;

    int similarDocumentsLimit = DEFAULT_SIMILAR_DOCUMENTS_LIMIT;

  }

  private static final Map<String, Function<Metadata, String>> metaFieldExtractor = Map.ofEntries(
    Map.entry(LuceneConstants.CREATOR, Metadata::getCreator),
    Map.entry(LuceneConstants.MODIFIER, Metadata::getModifier)
  );

  private static final List<DocumentEntry> retrievedIndexedDocuments = new ArrayList<>();

  @NotNull SimilarDocumentSourceParameters parameters;

  @NotNull LuceneIndex luceneIndex;

  public SimilarDocumentSource(
    @NotNull SimilarDocumentSourceParameters parameters,
    @NotNull LuceneIndex luceneIndex
  ) {
    this.parameters = parameters;
    this.luceneIndex = luceneIndex;
  }

  public List<DocumentEntry> retrieveSimilar(@NotNull DocumentEntry documentEntry)
    throws IndexingException {

    var similarDocs = this.luceneIndex.retrieveSimilar(
      documentEntry, parameters.getSimilarDocumentsLimit()
    );

    Metadata metadata = documentEntry.getFileData().getMetadata();

    for (var field : SimilarDocumentSource.metaFieldExtractor.keySet()) {
      var value = SimilarDocumentSource.metaFieldExtractor.get(field).apply(metadata);
      if (value != null) {
        similarDocs.addAll(
          this.luceneIndex.retrieveByField(field, value, parameters.getSimilarDocumentsLimit())
        );
      }
    }

    return similarDocs.stream()
      .map(LuceneResult::fileData)
      .map(f ->
        DocumentEntry.builder()
          .indexed(true)
          .fileData(f)
          .build()
      )
      .peek(SimilarDocumentSource.retrievedIndexedDocuments::add)
      .collect(Collectors.toList());
  }

}
