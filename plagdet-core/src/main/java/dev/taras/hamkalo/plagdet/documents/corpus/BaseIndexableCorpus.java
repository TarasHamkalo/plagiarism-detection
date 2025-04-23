package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseIndexableCorpus extends BaseCorpus implements IndexableCorpus {

  private static final double SIZE_RATIO_THRESHOLD = 0.5;

  @NotNull Embeddings embeddings;

  @NotNull Double targetSentenceCount;

  @Nullable SentenceIndex corpusSentenceIndex;

  @Nullable DocumentEntry cachedDocument;

  public BaseIndexableCorpus(
    @NotNull Embeddings embeddings,
    @NotNull FileDataSource fileDataSource,
    @NotNull Integer targetSentenceCount
  ) {
    super(fileDataSource);
    this.embeddings = embeddings;
    this.targetSentenceCount = 1d * targetSentenceCount;
  }

  @Override
  public @NotNull DocumentEntry next() throws CorpusAccessException {
    if (cachedDocument != null) {
      var next = cachedDocument;
      cachedDocument = null;
      return next;
    }

    return super.next();
  }

  @Override
  public boolean hasNext() throws CorpusAccessException {
    if (cachedDocument != null) {
      return true;
    }

    while (super.hasNext()) {
      var next = next();
      if (isSizeRatioMatched(next)) {
        cachedDocument = next;
        return true;
      } else {
        log.trace("Removing document from corpus, size was not matched");
        corpusDocuments.remove(cursor - 1);
        cursor--;
      }
    }

    return false;
  }

  public boolean isSizeRatioMatched(@NotNull DocumentEntry documentEntry) {
    double secondSize = AnnotationUtils.sentences(documentEntry.getAnnotation()).size();
    double sizeRatio = Math.min(targetSentenceCount / secondSize, secondSize / targetSentenceCount);
    return sizeRatio >= SIZE_RATIO_THRESHOLD;
  }

  @Override
  public @NotNull SentenceIndex getCorpusSentenceIndex() throws CorpusAccessException {
    if (corpusSentenceIndex == null) {
      try {
        corpusSentenceIndex = SentenceIndex.from(embeddings, this);
      } catch (PlagdetException e) {
        throw new CorpusAccessException("Was not able to index corpus documents");
      }
    }

    return corpusSentenceIndex;
  }

  @Override
  public int getCorpusSize() {
    return corpusDocuments.size();
  }

  @Override
  public void close() {
    if (corpusSentenceIndex != null) {
      try {
        corpusSentenceIndex.close();
      } catch (IOException ignore) {
      }
    }
  }
}
