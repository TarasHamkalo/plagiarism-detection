package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IndexingSequence {

  List<DocumentIndex> documentIndexes;

  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
    // TODO: exceptions ignored ?
    for (var index : documentIndexes) {
      documentEntry = index.pushToIndex(documentEntry);
    }

    return documentEntry;
  }

}
