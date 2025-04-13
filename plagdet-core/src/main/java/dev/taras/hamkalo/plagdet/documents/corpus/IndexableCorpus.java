package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import org.jetbrains.annotations.NotNull;

/**
 * Corpus is a container for preprocessed documents.
 * Implements kind of iterator for documents, providing one time iteration or reusable next.
 */
public interface IndexableCorpus extends Corpus {


  @NotNull SentenceIndex getCorpusSentenceIndex() throws CorpusAccessException;

  void close();

}
