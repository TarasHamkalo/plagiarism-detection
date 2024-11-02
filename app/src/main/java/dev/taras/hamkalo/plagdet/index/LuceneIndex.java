package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneIndex implements DocumentIndex {

  public static final Path STOP_WORDS_PATH = Path.of("app/src/main/resources/stopwords.txt");

  public static final Path LUCENE_INDEX_PATH = Path.of("lucene_index");

  Directory indexDirectory;

  // TODO: change analyzer, so that we can feed tokens from earlier stages
  StandardAnalyzer analyzer;

  @SneakyThrows
  public LuceneIndex() {
    // TODO: file closing and index closing
    this.indexDirectory = new MMapDirectory(LUCENE_INDEX_PATH);
    this.analyzer = new StandardAnalyzer(new FileReader(STOP_WORDS_PATH.toFile()));

    // TODO: lazy load
//    this.indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
//    this.indexReader = DirectoryReader.open(indexDirectory);
//    this.indexSearcher = new IndexSearcher(indexReader);
  }

  // TODO: exceptions for index closing
  @PreDestroy
  @SneakyThrows
  public void close() {
    this.indexDirectory.close();
//    this.indexReader.close();
//    this.indexWriter.close();
  }

  @Override
  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
    try (IndexWriter writer = new IndexWriter(indexDirectory, new IndexWriterConfig(analyzer))) {
      Document doc = new Document();
      doc.add(new LongPoint("id", documentEntry.getId()));
      doc.add(new StoredField("id", documentEntry.getId()));
      doc.add(new StringField("path", documentEntry.getPath().toString(), Field.Store.YES));

      var content = documentEntry.getContentFromTokens();

      doc.add(new TextField("content", content, Field.Store.YES));
      doc.add(new TextField("search_content", content, Field.Store.NO));

      var metadata = documentEntry.getMetadata();
      if (metadata != null) {
        doc.add(new StringField("authorName", metadata.getAuthorName(), Field.Store.YES));
//        doc.add(new IntPoint("totalEditTime", metadata.getTotalEditTime()));
        doc.add(new StoredField("totalEditTime", metadata.getTotalEditTime()));
      }

      writer.addDocument(doc);
    } catch (IOException e) {
      // TODO: exceptions handling index push
      throw new RuntimeException(e);
    }

    return documentEntry;

  }

  @Override
  public List<DocumentEntry> retrieveSimilarByContent(DocumentEntry documentEntry) {
    List<DocumentEntry> results = new ArrayList<>();
    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);

      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{"content"});
      // TODO: adjust min term freq
      mlt.setMinTermFreq(1);
      mlt.setMinDocFreq(1);
      // TODO: there is also processing -> tokens -> text -> tokens -> vector
      mlt.setAnalyzer(analyzer);

      var stringReader = new StringReader(documentEntry.getContentFromTokens());
      Query query = mlt.like("search_content", stringReader);
      TopDocs similarDocs = searcher.search(query, 11);
      for (ScoreDoc scoreDoc : similarDocs.scoreDocs) {
        Document doc = searcher.doc(scoreDoc.doc);
        System.out.println(doc.get("path") + ": " + scoreDoc.score);
        results.add(convertDocumentToDocumentEntry(doc));
      }

    } catch (IOException e) {
      // TODO: Exceptions LUCENE
      throw new RuntimeException(e);
    }
    return results;
  }

  @Override
  public DocumentEntry retrieveSimilarByMeta(Metadata metadata) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  private DocumentEntry convertDocumentToDocumentEntry(Document doc) {
    DocumentEntry entry = new DocumentEntry(doc.get("path"));
    entry.setContent(doc.get("content"));

    Metadata metadata = new Metadata(
      doc.get("authorName"),
      Integer.parseInt(doc.get("totalEditTime"))
    );
    entry.setMetadata(metadata);

    return entry;
  }
}
