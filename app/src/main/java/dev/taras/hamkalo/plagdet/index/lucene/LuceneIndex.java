package dev.taras.hamkalo.plagdet.index.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.index.DocumentIndex;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneIndex {

  public static final Path LUCENE_INDEX_PATH = Path.of("lucene_index");

  Directory indexDirectory = new MMapDirectory(LUCENE_INDEX_PATH);

  public LuceneIndex() throws IOException {
  }

  public enum DocumentFieldNames {
    ID, PATH, CONTENT, SEARCH_CONTENT
  }

  public record LuceneResult(DocumentEntry documentEntry, double score) {}

  @PreDestroy
  public void close() throws IOException {
    indexDirectory.close();
  }

  public void pushToIndex(DocumentEntry documentEntry) {
    var conf = new IndexWriterConfig(new DocumentEntryAnalyzer(documentEntry));
    try (IndexWriter writer = new IndexWriter(indexDirectory, conf)) {
      Document doc = new Document();
      doc.add(new StringField(
        DocumentFieldNames.PATH.toString(), documentEntry.getContentPath(), Field.Store.YES
      ));

      doc.add(new TextField(DocumentFieldNames.SEARCH_CONTENT.toString(), "", Field.Store.NO));

      writer.addDocument(doc);
    } catch (IOException e) {
      throw new RuntimeException("Failed to index document", e);
    }
  }

  /**
   *
   * @param documentEntry
   * @return document and similarity score for given query
   */
  public List<LuceneResult> retrieveSimilar(DocumentEntry documentEntry) {
    List<LuceneResult> results = new ArrayList<>();

    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{DocumentFieldNames.SEARCH_CONTENT.toString()});
      mlt.setAnalyzer(new DocumentEntryAnalyzer(documentEntry));

      Query query = mlt.like(DocumentFieldNames.SEARCH_CONTENT.toString(), new StringReader(""));
      TopDocs similarDocs = searcher.search(query, 10);

      for (ScoreDoc scoreDoc : similarDocs.scoreDocs) {
        var doc = searcher.doc(scoreDoc.doc);
        results.add(
          new LuceneResult(convertDocumentToDocumentEntry(doc), scoreDoc.score)
        );
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve similar documents", e);
    }

    return results;
  }

  private DocumentEntry convertDocumentToDocumentEntry(Document doc) {
    return new DocumentEntry(doc.get(DocumentFieldNames.PATH.toString()));
  }
}
