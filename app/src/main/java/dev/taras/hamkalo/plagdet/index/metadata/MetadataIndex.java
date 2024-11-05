package dev.taras.hamkalo.plagdet.index.metadata;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.index.DocumentIndex;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

// TODO: add exceptions
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MetadataIndex implements DocumentIndex {

  DocumentRepository documentRepository;

  @Override
  public void pushToIndex(DocumentEntry documentEntry) {
    // TODO: there should not be any
    // TODO: check for duplicates and throw exception
    documentRepository.save(DocumentEntity.from(documentEntry));
  }

  @Override
  public List<DocumentEntry> retrieveSimilar(DocumentEntry documentEntry) {
    // TODO: handle "Unknown", limit to 10
    var authorName = documentEntry.getMetadata().getAuthorName();
    var documents = documentRepository.findByAuthorName(authorName);
    if (documents.isEmpty()) {
      return Collections.emptyList();
    }

    return documents.stream().map(MetadataIndex::entityToDocumentEntry).toList();
  }

  private static DocumentEntry entityToDocumentEntry(DocumentEntity entity) {
    var meta = new Metadata(
      entity.getAuthorName(), entity.getCreationDate(), entity.getTotalEditTime()
    );

    var targetDocumentEntry = new DocumentEntry(entity.getFileStoragePath());
    targetDocumentEntry.setMetadata(meta);

    return targetDocumentEntry;
  }

}
