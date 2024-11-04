package dev.taras.hamkalo.plagdet.index.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.index.DocumentIndex;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.document.*;
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
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneIndex implements DocumentIndex {

  public static final Path LUCENE_INDEX_PATH = Path.of("lucene_index");

  public enum DocumentFieldNames {
    ID, PATH, CONTENT, SEARCH_CONTENT
//    ID("id"), PATH("path"), CONTENT("content"), SEARCH_CONTENT("search_content");
//    String val;
  }

  Directory indexDirectory;

  @SneakyThrows
  public LuceneIndex() {
    this.indexDirectory = new MMapDirectory(LUCENE_INDEX_PATH);
  }

  @PreDestroy
  @SneakyThrows
  public void close() {
    // TODO: exceptions for index closing
    this.indexDirectory.close();
  }

  @Override
  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
    var conf = new IndexWriterConfig(new DocumentEntryAnalyzer(documentEntry));
    try (IndexWriter writer = new IndexWriter(indexDirectory, conf)) {
      Document doc = new Document();
      doc.add(new StringField(
        DocumentFieldNames.PATH.toString(),
        documentEntry.getPath().toString(),
        Field.Store.YES
      ));

      doc.add(new TextField(DocumentFieldNames.SEARCH_CONTENT.toString(), "", Field.Store.NO)); // field value is ignore
      writer.addDocument(doc);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return documentEntry;
  }

  @Override
  public List<DocumentEntry> retrieveSimilar(DocumentEntry documentEntry) {
    List<DocumentEntry> results = new ArrayList<>();
    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);

      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{DocumentFieldNames.SEARCH_CONTENT.toString()});
      // TODO: adjust min term freq
//      mlt.setMinTermFreq(0);
      mlt.setAnalyzer(new DocumentEntryAnalyzer(documentEntry));

      var stringReader = new StringReader("");
      Query query = mlt.like(DocumentFieldNames.SEARCH_CONTENT.toString(), stringReader);
      TopDocs similarDocs = searcher.search(query, 11);
      for (ScoreDoc scoreDoc : similarDocs.scoreDocs) {
        Document doc = searcher.doc(scoreDoc.doc);
//        System.out.println(searcher.explain(query, scoreDoc.doc));
        System.out.println(doc.get(DocumentFieldNames.PATH.toString()) + ": " + scoreDoc.score);
        results.add(convertDocumentToDocumentEntry(doc));
      }

    } catch (IOException e) {
      // TODO: Exceptions LUCENE
      throw new RuntimeException(e);
    }

    return results;
  }

  private DocumentEntry convertDocumentToDocumentEntry(Document doc) {
    var entry = new DocumentEntry(doc.get(DocumentFieldNames.PATH.toString()));
    return entry;
  }
}
