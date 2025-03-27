package dev.taras.hamkalo.plagdet.indexing;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneResult;
import dev.taras.hamkalo.plagdet.io.Metadata;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SimilarDocumentSource {

  @Value
  @Builder
  @AllArgsConstructor
  public static class SimilarDocumentSourceParameters {

    public static final int DEFAULT_SIMILAR_DOCUMENTS_LIMIT = 10;

    public static final int DEFAULT_MAX_META_SIMILAR_DOC = 3;

    @Builder.Default
    int similarDocumentsLimit = DEFAULT_SIMILAR_DOCUMENTS_LIMIT;

    @Builder.Default
    int maxMetaSimilarDoc = DEFAULT_MAX_META_SIMILAR_DOC;
  }

  private static final Map<String, Function<Metadata, String>> metaFieldExtractor = Map.ofEntries(
    Map.entry(LuceneConstants.CREATOR, Metadata::getCreator),
    Map.entry(LuceneConstants.MODIFIER, Metadata::getModifier)
  );

  @Getter
  private static final Map<Integer, DocumentEntry> retrievedDocumentsCache = new ConcurrentHashMap<>();

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
    if (this.parameters.getSimilarDocumentsLimit() <= 0) {
      return new ArrayList<>();
    }

    var similarDocs = querySimilarFor(documentEntry);
    var similarDocumentEntries = new HashMap<Integer, DocumentEntry>(); // deduplicate by id
    for (var similarDoc : similarDocs) {
      var documentId = DocumentEntry.getId(similarDoc.fileData());
      var cached = getCachedDocumentEntry(similarDoc, documentId);
      similarDocumentEntries.put(documentId, cached);
    }

    return new ArrayList<>(similarDocumentEntries.values());
  }

  private @NotNull List<LuceneResult> querySimilarFor(@NotNull DocumentEntry documentEntry) {
    var similarDocs = this.luceneIndex.retrieveSimilar(
      documentEntry, parameters.getSimilarDocumentsLimit()
    );

    Metadata metadata = documentEntry.getFileData().getMetadata();

    for (var field : SimilarDocumentSource.metaFieldExtractor.keySet()) {
      var value = SimilarDocumentSource.metaFieldExtractor.get(field).apply(metadata);
      if (value != null) {
        similarDocs.addAll(
          this.luceneIndex.retrieveByField(field, value, parameters.getMaxMetaSimilarDoc())
        );
      }
    }
    return similarDocs;
  }

  private static DocumentEntry getCachedDocumentEntry(LuceneResult similarDoc, int documentId) {
    return retrievedDocumentsCache.computeIfAbsent(documentId, (key) ->
      DocumentEntry.builder()
        .indexed(true)
        .fileData(similarDoc.fileData())
        .build()
    );
  }

}
