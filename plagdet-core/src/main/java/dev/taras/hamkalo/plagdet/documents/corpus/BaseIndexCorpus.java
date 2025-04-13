package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseIndexCorpus extends BaseCorpus implements IndexedCorpus {

  @NotNull Embeddings embeddings;

  @Nullable SentenceIndex corpusSentenceIndex;

  public BaseIndexCorpus(
    @NotNull Embeddings embeddings,
    @NotNull FileDataSource fileDataSource
  ) {
    super(fileDataSource);
    this.embeddings = embeddings;
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
  public void close() {
    if (corpusSentenceIndex != null) {
      try {
        corpusSentenceIndex.close();
      } catch (IOException ignore) {
      }
    }
  }
}
