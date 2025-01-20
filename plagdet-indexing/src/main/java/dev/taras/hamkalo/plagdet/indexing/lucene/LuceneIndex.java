package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.index.lucene.DocumentEntryAnalyzer;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.io.Metadata;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneIndex {

  public static final Path DEFAULT_INDEX_DIR_PATH = Path.of("lucene_index");

  Directory indexDirectory;

  public LuceneIndex(String indexDirectoryPath) throws IOException {
    this.indexDirectory = MMapDirectory.open(
      indexDirectoryPath == null ? DEFAULT_INDEX_DIR_PATH : Path.of(indexDirectoryPath)
    );
  }

  public void addToIndex(DocumentEntry document) {
    var conf = new IndexWriterConfig(new DocumentAnalyzer(document, true));
    try (IndexWriter writer = new IndexWriter(this.indexDirectory, conf)) {
      Document doc = new Document();

      var fileData = document.getFileData();

      addFileData(doc, fileData);

      addMetadata(fileData.getMetadata(), doc);

      writer.addDocument(doc);
    } catch (IOException e) {
      throw new RuntimeException("Failed to index document", e);
    }
  }

  private static void addFileData(Document doc, FileData fileData) {
    doc.add(new StringField(
      DocumentFieldNames.PATH.toString(), fileData.getPath().toString(), Field.Store.YES
    ));
    doc.add(new TextField(DocumentFieldNames.SEARCH_CONTENT.toString(), "", Field.Store.NO));
    doc.add(new TextField(
      DocumentFieldNames.CONTENT.toString(), fileData.getContent(), Field.Store.YES
    ));
    doc.add(new StringField(
      DocumentFieldNames.SUBMITTER.toString(), fileData.getSubmitter(), Field.Store.YES
    ));
  }

  private static void addMetadata(Metadata metadata, Document doc) {
    doc.add(new StringField(
      DocumentFieldNames.CREATOR.toString(), metadata.getCreator(), Field.Store.YES
    ));
    doc.add(new StringField(
      DocumentFieldNames.CREATION_DATE.toString(),
      metadata.getCreationDate().toString(),
      Field.Store.YES
    ));
    doc.add(new LongPoint(
      DocumentFieldNames.TOTAL_EDIT_TIME.toString(),
      metadata.getTotalEditTime()
    ));
  }

  public List<LuceneResult> retrieveSimilar(DocumentEntry document, int limit) {
    List<LuceneResult> results = new ArrayList<>();

    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{DocumentFieldNames.SEARCH_CONTENT.toString()});
      mlt.setAnalyzer(new DocumentEntryAnalyzer(document));

      Query query = mlt.like(DocumentFieldNames.SEARCH_CONTENT.toString(), new StringReader(""));
      TopDocs similarDocs = searcher.search(query, limit);

      for (ScoreDoc scoreDoc : similarDocs.scoreDocs) {
        var doc = searcher.doc(scoreDoc.doc);
        results.add(
          new LuceneResult(parseFileData(doc), scoreDoc.score)
        );
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve similar documents", e);
    }

    return results;
  }

  public List<FileData> searchByCreator(String creator, int limit) {
    List<FileData> results = new ArrayList<>();
    try (DirectoryReader reader = DirectoryReader.open(this.indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      Query query = new TermQuery(new Term(DocumentFieldNames.CREATOR.toString(), creator));

      TopDocs topDocs = searcher.search(query, limit);
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        results.add(parseFileData(searcher.doc(scoreDoc.doc)));
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to search index", e);
    }

    return results;
  }

  private static FileData parseFileData(Document doc) throws IOException {
    return FileData.builder()
      .submitter(doc.get(DocumentFieldNames.SUBMITTER.toString()))
      .path(Path.of(doc.get(DocumentFieldNames.PATH.toString())))
      .content(doc.get(DocumentFieldNames.CONTENT.toString()))
      .metadata(parseMetadata(doc))
      .build();
  }

  private static Metadata parseMetadata(Document doc) {
    return Metadata.builder()
      .creator(doc.get(DocumentFieldNames.CREATOR.toString()))
      .creationDate(Date.valueOf(doc.get(DocumentFieldNames.CREATION_DATE.toString())))
      .totalEditTime(Long.valueOf(doc.get(DocumentFieldNames.TOTAL_EDIT_TIME.toString())))
      .build();
  }

}
