package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import org.jetbrains.annotations.NotNull;

/**
 * Corpus is a container for preprocessed documents.
 * Implements kind of iterator for documents, providing one time iteration or reusable next.
 */
public interface Corpus {

  @NotNull DocumentEntry at(int position) throws CorpusAccessException;

  @NotNull DocumentEntry pop() throws CorpusAccessException;

  @NotNull DocumentEntry next() throws CorpusAccessException;

  boolean hasAny();

  boolean hasNext();

  void reset();

}
