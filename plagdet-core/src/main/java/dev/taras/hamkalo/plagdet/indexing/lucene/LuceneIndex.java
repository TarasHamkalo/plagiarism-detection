package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.exception.LuceneException;
import dev.taras.hamkalo.plagdet.indexing.lucene.analysis.DocumentAnalyzer;
import dev.taras.hamkalo.plagdet.indexing.lucene.mapping.FileDataMapper;
import dev.taras.hamkalo.plagdet.indexing.lucene.mapping.MetadataMapper;
import dev.taras.hamkalo.plagdet.indexing.lucene.mapping.StanfordAnnotationMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LuceneIndex {

  @NotNull
  Directory indexDirectory;

  @NotNull
  Path indexDirectoryPath;

  @Nullable
  DirectoryReader reader;

  @Nullable
  IndexWriter writer;

  @NotNull
  Map<String, Double> inverseDocumentFrequencyCache;

  public LuceneIndex(@NotNull Path indexDirectoryPath) throws IOException {
    this.indexDirectoryPath = indexDirectoryPath;
    this.indexDirectory = MMapDirectory.open(indexDirectoryPath);
    this.inverseDocumentFrequencyCache = new HashMap<>();
  }

  public void addToIndex(@NotNull DocumentEntry document) {
    try {
      initIndexWriter();
      if (writer.getConfig().getAnalyzer() instanceof DocumentAnalyzer analyzer) {
        analyzer.setDocument(document);
      }

      Document doc = new Document();
      log.info("Indexing document with path {}", document.getFileData().getPersistentPath());
      final var duplicateDocuments = retrieveByField(
        LuceneConstants.ID, String.valueOf(document.getId()), 1
      );
      log.trace("Duplicate list {}", duplicateDocuments);
      if (!duplicateDocuments.isEmpty()) {
        log.info(
          "Indexing for document {} aborted, already indexed",
          document.getFileData().getPersistentPath()
        );
        return;
      }

      var fileData = document.getFileData();
      FileDataMapper.addId(document.getId(), doc);
      FileDataMapper.addFileData(fileData, doc);
      MetadataMapper.addMetadata(fileData.getMetadata(), doc);
      StanfordAnnotationMapper.addPreprocessedContent(document, doc);
      writer.addDocument(doc);
      writer.commit();
    } catch (IOException e) {
      throw new LuceneException("Failed to index document", e);
    }
  }

  public @NotNull List<LuceneResult> retrieveSimilar(@NotNull DocumentEntry document, int limit) {
    try {
      initIndexReader();
      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{LuceneConstants.SEARCH_CONTENT});
      mlt.setAnalyzer(new DocumentAnalyzer(document));
      Query query = mlt.like(LuceneConstants.SEARCH_CONTENT, new StringReader(""));
      return executeSearch(query, limit, reader);
    } catch (IndexNotFoundException error) {
      log.info("Index was empty while retrieving similar");
      return new ArrayList<>();
    } catch (IOException e) {
      throw new LuceneException("Failed to retrieve similar documents", e);
    }
  }

  public @NotNull List<LuceneResult> retrieveByField(
    @NotNull String field,
    @NotNull String value,
    int limit
  ) throws LuceneException {
    try {
      initIndexReader();
      Query query = new TermQuery(new Term(field, value));
      return executeSearch(query, limit, reader);
    } catch (IndexNotFoundException error) {
      log.info("Index was empty while retrieving by field");
      return new ArrayList<>();
    } catch (IOException e) {
      throw new LuceneException("Failed to search index", e);
    }
  }

  private void initIndexWriter() throws IOException {
    if (writer == null) {
      var conf = new IndexWriterConfig(new DocumentAnalyzer());
      writer = new IndexWriter(this.indexDirectory, conf);
    }
  }

  private void initIndexReader() throws IOException {
    if (reader == null) {
      log.trace("Opening new reader");
      reader = DirectoryReader.open(indexDirectory);
    }
  }

  private List<LuceneResult> executeSearch(Query query, int limit, IndexReader reader) throws IOException {
    IndexSearcher searcher = new IndexSearcher(reader);
    List<LuceneResult> results = new ArrayList<>();
    TopDocs topDocs = searcher.search(query, limit);

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      Document doc = searcher.doc(scoreDoc.doc);
      results.add(new LuceneResult(FileDataMapper.parseFileData(doc), scoreDoc.score));
    }
    return results;
  }


  public double inverseDocumentFrequency(@NotNull String term) {
    return inverseDocumentFrequencyCache.computeIfAbsent(
      term.toLowerCase(), this::computeIdf
    );
  }

  private double computeIdf(@NotNull String term) {
    try {
      initIndexReader();

      double totalDocs = reader.numDocs() * 1.0;
      int docFreq = reader.docFreq(new Term(LuceneConstants.SEARCH_CONTENT, term));
      if (docFreq == 0) {
        return 1 + Math.log(totalDocs + 1);
      }

      return 1 + Math.log((totalDocs) / (docFreq + 1));
    } catch (IOException e) {
      throw new LuceneException("Failed to retrieve term frequency", e);
    }
  }

  public void close() {
    try {
      if (writer != null) {
        writer.close();
      }
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      log.warn("Failed to close IndexReader", e);
    }
  }
}
