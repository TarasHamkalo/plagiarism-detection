package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;

import java.util.List;

public interface DocumentIndex {

  void pushToIndex(DocumentEntry documentEntry);

  List<DocumentEntry> retrieveSimilar(DocumentEntry documentEntry);

}
