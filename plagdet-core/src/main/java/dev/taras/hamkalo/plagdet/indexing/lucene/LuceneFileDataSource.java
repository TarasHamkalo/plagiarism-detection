package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.indexing.exception.LuceneException;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LuceneFileDataSource implements FileDataSource {

  /**
   * Used for iterative way of access multiple metadata fields
   */
  private static final Map<String, Function<Metadata, String>> METADATA_FIELD_EXTRACTOR =
    Map.ofEntries(
      Map.entry(LuceneConstants.CREATOR, Metadata::getCreator),
      Map.entry(LuceneConstants.MODIFIER, Metadata::getModifier)
    );

  @Value
  @Builder
  public static class LuceneFileDataSourceParameters {

    public static final int DEFAULT_SIMILAR_DOCUMENTS_LIMIT = 10;

    /**
     * Amount of queried documents per filed with same metadata
     */
    public static final int DEFAULT_MAX_META_SIMILAR_DOC = 3;

    @Builder.Default
    int similarDocumentsLimit = DEFAULT_SIMILAR_DOCUMENTS_LIMIT;

    @Builder.Default
    int maxMetadataSimilarDoc = DEFAULT_MAX_META_SIMILAR_DOC;
  }

  @NotNull LuceneIndex luceneIndex;

  @NotNull DocumentEntry suspiciousDocument;

  @Nullable Queue<FileData> similarDocuments;

  @NotNull LuceneFileDataSourceParameters parameters;

  @Override
  public @NotNull List<FileData> all() throws FileDataSourceException {
    if (similarDocuments == null) {
      similarDocuments = new LinkedList<>(retrieveSimilar());
    }

    return new ArrayList<>(similarDocuments);
  }

  @Override
  public @NotNull FileData next() throws FileDataSourceException {
    if (similarDocuments == null) {
      similarDocuments = new LinkedList<>(retrieveSimilar());
    }

    FileData fileData = similarDocuments.poll();
    if (fileData != null) {
      return fileData;
    }

    throw new FileDataSourceException("FileData source is empty");
  }

  @Override
  public boolean hasNext() throws FileDataSourceException {
    if (similarDocuments == null) {
      similarDocuments = new LinkedList<>(retrieveSimilar());
    }

    return !similarDocuments.isEmpty();
  }

  public @NotNull List<FileData> retrieveSimilar() throws LuceneException {
    var similarDocs = querySimilar();
    log.trace("Total count of retrieved documents is {}", similarDocs.size());
    var similarDocumentEntries = new HashMap<Integer, FileData>();
    for (var similarDoc : similarDocs) {
      var documentId = DocumentEntry.getId(similarDoc.fileData());
      similarDocumentEntries.put(documentId, similarDoc.fileData());
    }

    return new ArrayList<>(similarDocumentEntries.values());
  }

  private @NotNull List<LuceneResult> querySimilar() {
    List<LuceneResult> similarDocs = new ArrayList<>();
    if (parameters.similarDocumentsLimit > 0) {
      log.info("Querying Lucene for similar documents");
      similarDocs.addAll(this.luceneIndex.retrieveSimilar(
        suspiciousDocument, parameters.getSimilarDocumentsLimit()
      ));
      log.info("Querying Lucene for similar documents done");
    }

    if (parameters.maxMetadataSimilarDoc <= 0) {
      return similarDocs;
    }

    log.trace("Querying Lucene for documents with metadata");
    Metadata metadata = suspiciousDocument.getFileData().getMetadata();
    for (var field : METADATA_FIELD_EXTRACTOR.keySet()) {
      var value = METADATA_FIELD_EXTRACTOR.get(field).apply(metadata);
      if (value != null) {
        similarDocs.addAll(
          this.luceneIndex.retrieveByField(field, value, parameters.getMaxMetadataSimilarDoc())
        );
      }
    }
    log.trace("Querying Lucene for documents with metadata done");
    return similarDocs;
  }

  @Override
  public int estimateFilesCount() throws FileDataSourceException {
    if (similarDocuments == null) {
      return 0;
    }

    return similarDocuments.size();
  }

}
