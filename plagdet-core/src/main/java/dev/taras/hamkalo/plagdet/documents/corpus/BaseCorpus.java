package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.exceptions.CorpusAccessException;
import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseCorpus implements Corpus {

  @NotNull FileDataSource fileDataSource;

  @NotNull List<DocumentEntry> corpusDocuments = new ArrayList<>();

  @NotNull Integer cursor = 0;

  @Override
  public @NotNull Map<Integer, DocumentEntry> getDocumentsMap() throws CorpusAccessException {
    Map<Integer, DocumentEntry> documentsMap = new HashMap<>(corpusDocuments.size());
    corpusDocuments.forEach(d -> documentsMap.put(d.getId(), d));
    return documentsMap;
  }

  @Override
  public @NotNull DocumentEntry at(int position) throws CorpusAccessException {
    if (position < corpusDocuments.size()) {
      return corpusDocuments.get(position);
    }

    while (hasNext()) {
      var next = next();
      if (position < corpusDocuments.size()) {
        return next;
      }
    }

    throw new CorpusAccessException("Invalid corpus position " + position);
  }

  @Override
  public @NotNull DocumentEntry pop() throws CorpusAccessException {
    var next = next();
    corpusDocuments.remove(cursor - 1);
    cursor--;
    return next;
  }

  @Override
  public boolean hasAny() throws CorpusAccessException {
    return !corpusDocuments.isEmpty() || fileDataSource.hasNext();
  }

  @Override
  public boolean hasNext() throws CorpusAccessException {
    return cursor < corpusDocuments.size() || fileDataSource.hasNext();
  }

  @Override
  public @NotNull DocumentEntry next() throws CorpusAccessException {
    try {
      if (cursor >= corpusDocuments.size() && fileDataSource.hasNext()) {
        var next = fileDataSource.next();
        var annotation = Pipeline.annotate(next.getContent());
        corpusDocuments.add(
          DocumentEntry.builder()
            .fileData(next)
            .annotation(annotation)
            .build()
        );
      }

      return corpusDocuments.get(cursor++);
    } catch (IndexOutOfBoundsException | FileDataSourceException e) {
      throw new CorpusAccessException(e);
    }
  }

  @Override
  public void reset() {
    cursor = 0;
  }
}
