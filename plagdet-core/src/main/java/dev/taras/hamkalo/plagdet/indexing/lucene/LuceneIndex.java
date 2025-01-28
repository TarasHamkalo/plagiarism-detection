package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.io.FileData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// TODO: deduplication, check for id
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneIndex {

  @NotNull Directory indexDirectory;

  @Getter
  Path indexDirectoryPath;

  public LuceneIndex(@NotNull Path indexDirectoryPath) throws IOException {
    this.indexDirectoryPath = indexDirectoryPath;
    this.indexDirectory = MMapDirectory.open(indexDirectoryPath);
  }

  public void addToIndex(@NotNull DocumentEntry document) {
    var conf = new IndexWriterConfig(new DocumentAnalyzer(document, true));
    try (IndexWriter writer = new IndexWriter(this.indexDirectory, conf)) {
      Document doc = new Document();
      log.info("Indexing document with path {}", document.getFileData().getPersistentPath());
      final var duplicateDocuments = retrieveByField(
        LuceneConstants.ID, String.valueOf(document.getId()), 1
      );
      log.debug("Duplicate list {}" , duplicateDocuments);
      if (!duplicateDocuments.isEmpty()) {
        log.info("Indexing for document with id {} aborted, already indexed", document.getId());
        return;
      }

      var fileData = document.getFileData();
      FileDataMapper.addId(document.getId(), doc);
      FileDataMapper.addFileData(fileData, doc);
      MetadataMapper.addMetadata(fileData.getMetadata(), doc);
      writer.addDocument(doc);
    } catch (IOException e) {
      throw new IndexingException("Failed to index document", e);
    }
  }

  public @NotNull List<LuceneResult> retrieveSimilar(@NotNull DocumentEntry document, int limit) {
    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{LuceneConstants.SEARCH_CONTENT});
      mlt.setAnalyzer(new DocumentAnalyzer(document, true));

      Query query = mlt.like(LuceneConstants.SEARCH_CONTENT, new StringReader(""));
      return executeSearch(query, limit, reader);
    } catch (IndexNotFoundException error) {
      log.error("Index is empty");
      return new ArrayList<>();
    } catch (IOException e) {
      throw new IndexingException("Failed to retrieve similar documents", e);
    }
  }

  public @NotNull List<LuceneResult> retrieveByField(
    @NotNull String field,
    @NotNull String value,
    int limit
  ) throws IndexingException {
    try (DirectoryReader reader = DirectoryReader.open(this.indexDirectory)) {
      Query query = new TermQuery(new Term(field, value));

      return executeSearch(query, limit, reader);
    } catch (IndexNotFoundException error) {
      log.error("Index is empty");
      return new ArrayList<>();
    } catch (IOException e) {
      throw new IndexingException("Failed to search index", e);
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

  public double inverseTermFrequency(@NotNull String term) {
    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      double totalDocs = reader.numDocs() * 1.0;
      int docFreq = reader.docFreq(new Term(LuceneConstants.SEARCH_CONTENT, term));
      if (docFreq == 0) {
        return Math.log(totalDocs + 1);
      }

      return Math.log((totalDocs) / (docFreq + 1));
    } catch (IOException e) {
      throw new IndexingException("Failed to retrieve term frequency", e);
    }
  }

}
