package dev.taras.hamkalo.plagdet.documents.pool;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.exceptions.DocumentPoolAccessException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import edu.stanford.nlp.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;

public interface DocumentPool {

  @NotNull DocumentEntry nextSuspiciousDocument() throws DocumentPoolAccessException;

  @NotNull DocumentEntry nextSourceDocument() throws DocumentPoolAccessException;

  @NotNull DocumentEntry getSuspiciousDocument() throws DocumentPoolAccessException;

  @NotNull DocumentEntry getSourceDocument() throws DocumentPoolAccessException;

  @Nullable SentenceIndex getSuspiciousDocumentSentenceIndex();

  @Nullable List<Pair<INDArray, INDArray>> getSuspiciousDocumentSentenceMatrices();

  void prepareSuspiciousDocument() throws DocumentPoolAccessException;

  boolean hasAnySuspiciousDocument();

  boolean hasAnySourceDocument();

}
