package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.io.Metadata;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
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
    doc.add(new StringField(LuceneConstants.PATH, fileData.getPath().toString(), Store.YES));
    doc.add(new TextField(LuceneConstants.CONTENT, fileData.getContent(), Store.YES));
    doc.add(new TextField(LuceneConstants.SEARCH_CONTENT, "", Store.NO));
    doc.add(new StringField(LuceneConstants.SUBMITTER, fileData.getSubmitter(), Store.YES));
  }

  private static void addMetadata(Metadata metadata, Document doc) {
    doc.add(new StringField(LuceneConstants.CREATOR, metadata.getCreator(), Store.YES));

    Optional.ofNullable(metadata.getCreationDate()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.CREATION_DATE, dateString(date), Store.YES));
    });

    var totalEditTime = metadata.getTotalEditTime().toString();
    doc.add(new StringField(LuceneConstants.TOTAL_EDIT_TIME, totalEditTime, Store.YES));

    doc.add(new StringField(LuceneConstants.MODIFIER, metadata.getModifier(), Store.YES));

    Optional.ofNullable(metadata.getModificationDate()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.MODIFICATION_DATE, dateString(date), Store.YES));
    });

    Optional.ofNullable(metadata.getLastPrinted()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.LAST_PRINTED, dateString(date), Store.YES));
    });

    var revisionsNumber = metadata.getRevisionsNumber().toString();
    doc.add(new StringField(LuceneConstants.REVISIONS_NUMBER, revisionsNumber, Store.YES));
  }

  private static String dateString(Date date) {
    return DateTools.dateToString(date, DateTools.Resolution.SECOND);
  }

  public List<LuceneResult> retrieveSimilar(DocumentEntry document, int limit) {
    try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
      MoreLikeThis mlt = new MoreLikeThis(reader);
      mlt.setFieldNames(new String[]{LuceneConstants.SEARCH_CONTENT});
      mlt.setAnalyzer(new DocumentAnalyzer(document, true));

      Query query = mlt.like(LuceneConstants.SEARCH_CONTENT, new StringReader(""));
      return executeSearch(query, limit, reader);
    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve similar documents", e);
    }
  }

  public List<FileData> retrieveByCreator(String creator, int limit) {
    List<FileData> results = new ArrayList<>();
    try (DirectoryReader reader = DirectoryReader.open(this.indexDirectory)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      Query query = new TermQuery(new Term(LuceneConstants.CREATOR, creator));

      TopDocs topDocs = searcher.search(query, limit);
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        results.add(parseFileData(searcher.doc(scoreDoc.doc)));
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to search index", e);
    }

    return results;
  }

  private List<LuceneResult> executeSearch(Query query, int limit, IndexReader reader) throws IOException {
    IndexSearcher searcher = new IndexSearcher(reader);
    List<LuceneResult> results = new ArrayList<>();
    TopDocs topDocs = searcher.search(query, limit);

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      Document doc = searcher.doc(scoreDoc.doc);
      results.add(new LuceneResult(parseFileData(doc), scoreDoc.score));
    }
    return results;
  }

  private static FileData parseFileData(Document doc) throws IOException {
    return FileData.builder()
      .submitter(doc.get(LuceneConstants.SUBMITTER))
      .path(Path.of(doc.get(LuceneConstants.PATH)))
      .content(doc.get(LuceneConstants.CONTENT))
      .metadata(parseMetadata(doc))
      .build();
  }

  private static Metadata parseMetadata(Document doc) {
    Date creationDate = parseDate(doc.get(LuceneConstants.CREATION_DATE));
    Date modificationDate = parseDate(doc.get(LuceneConstants.MODIFICATION_DATE));
    Date lastPrinted = parseDate(doc.get(LuceneConstants.LAST_PRINTED));

    return Metadata.builder()
      .creator(doc.get(LuceneConstants.CREATOR))
      .creationDate(creationDate)
      .totalEditTime(parseLong(doc.get(LuceneConstants.TOTAL_EDIT_TIME), 0L))
      .revisionsNumber(parseInt(doc.get(LuceneConstants.REVISIONS_NUMBER), 0))
      .modificationDate(modificationDate)
      .lastPrinted(lastPrinted)
      .modifier(doc.get(LuceneConstants.MODIFIER))
      .build();
  }

  private static long parseLong(String value, long defaultValue) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static int parseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static Date parseDate(String dateString) {
    if (dateString == null) {
      return null;
    }

    try {
      return DateTools.stringToDate(dateString);
    } catch (Exception e) {
      log.warn("Invalid date format: {}", dateString, e);
      return null;
    }
  }
}
