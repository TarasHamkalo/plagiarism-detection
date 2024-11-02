package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;

import java.util.List;

public interface DocumentIndex {

  DocumentEntry pushToIndex(DocumentEntry documentEntry);

  List<DocumentEntry> retrieveSimilarByContent(DocumentEntry documentEntry);

  DocumentEntry retrieveSimilarByMeta(Metadata metadata);

}
