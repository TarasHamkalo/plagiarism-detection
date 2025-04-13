package dev.taras.hamkalo.plagdet.documents.pool;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.corpus.Corpus;
import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import dev.taras.hamkalo.plagdet.documents.exceptions.DocumentPoolAccessException;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseDocumentPool implements DocumentPool {

  @NotNull Corpus corpus;

  @NotNull Embeddings embeddings;

  @Nullable DocumentEntry suspiciousDocument;

  @Nullable DocumentEntry sourceDocument;

  @Nullable DocumentEntry cachedNextSource;

  @Getter
  @Nullable
  SentenceIndex suspiciousDocumentSentenceIndex;

  @Getter
  @Nullable
  List<Pair<INDArray, INDArray>> suspiciousDocumentSentenceMatrices;

  @Override
  public @NotNull DocumentEntry getSuspiciousDocument() throws DocumentPoolAccessException {
    if (suspiciousDocument != null) {
      return suspiciousDocument;
    }

    throw new DocumentPoolAccessException("Suspicous document was not queried yet");
  }

  @Override
  public @NotNull DocumentEntry getSourceDocument() throws DocumentPoolAccessException {
    if (sourceDocument != null) {
      return sourceDocument;
    }

    throw new DocumentPoolAccessException("Suspicous document was not queried yet");
  }

  @Override
  public @NotNull DocumentEntry nextSuspiciousDocument() throws DocumentPoolAccessException {
    try {
      corpus.reset();
      suspiciousDocument = corpus.pop();
      closeResources();
      return suspiciousDocument;
    } catch (CorpusAccessException e) {
      throw new DocumentPoolAccessException(e);
    }
  }

  @Override
  public void prepareSuspiciousDocument() throws DocumentPoolAccessException {
    if (suspiciousDocument == null) {
      throw new DocumentPoolAccessException("Suspicious document was not queried yet");
    }

    try {
      suspiciousDocumentSentenceIndex =
        SentenceIndex.from(embeddings, List.of(suspiciousDocument));
    } catch (PlagdetException e) {
      throw new DocumentPoolAccessException("Was not able to index suspicious document");
    }

    suspiciousDocumentSentenceMatrices = new ArrayList<>(
      AnnotationUtils.sentences(suspiciousDocument.getAnnotation()).stream()
        .map(embeddings::getWordVectorsWithWeights)
        .toList()
    );
  }

  private void closeResources() {
    cachedNextSource = null;
    if (suspiciousDocumentSentenceIndex != null) {
      try {
        suspiciousDocumentSentenceIndex.close();
      } catch (IOException ignore) {
      }
    }
  }

  @Override
  public @NotNull DocumentEntry nextSourceDocument() {
    if (cachedNextSource != null) {
      sourceDocument = cachedNextSource;
      cachedNextSource = null;
      return sourceDocument;
    }

    while (corpus.hasNext()) {
      var next = corpus.next();
      if (!hasSameSubmitter(next, suspiciousDocument)) {
        sourceDocument = next;
        return sourceDocument;
      }
    }

    throw new DocumentPoolAccessException("No source document present, perhaps invalid iterator");
  }

  @Override
  public boolean hasAnySuspiciousDocument() {
    return corpus.hasAny();
  }

  @Override
  public boolean hasAnySourceDocument() {
    if (cachedNextSource != null) {
      return true;
    }

    while (corpus.hasNext()) {
      var candidate = corpus.next();
      if (!hasSameSubmitter(candidate, suspiciousDocument)) {
        cachedNextSource = candidate;
        return true;
      }
    }

    return false;
  }


  private boolean hasSameSubmitter(@NotNull DocumentEntry that, @Nullable DocumentEntry other) {
    if (other == null) {
      return false;
    }

    return that.getFileData().getSubmitter().equals(other.getFileData().getSubmitter());
  }
}