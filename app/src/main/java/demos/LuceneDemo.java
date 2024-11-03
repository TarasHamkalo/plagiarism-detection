package demos;

import dev.taras.hamkalo.plagdet.readers.WordDocumentReader;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

//https://github.com/dbpedia-spotlight/lucene-quickstarter/blob/master/i18n/sk/stopwords.list

//https://ishanupamanyu.com/blog/apache-lucene-tutorial/
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LuceneDemo {

  public static final Path STOP_WORDS_PATH = Path.of("app/src/main/resources/stopwords.txt");

  public static final Path DOCX_20 = Path.of(
    "/home/taras-hamkalo/repositories/plagiarism-detection/app/src/main/resources/20.docx"
  );

  @SneakyThrows
  public static void main(String[] args) {
//    writeToIndex();
    readIndex();
  }

  private static void readIndex() throws IOException, ParseException {
    Directory indexDirectory = new MMapDirectory(Path.of("lucene_index"));
    StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(STOP_WORDS_PATH.toFile()));
    IndexReader indexReader = DirectoryReader.open(indexDirectory);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //This query parser will search in title field by default if no field is specified.
    //Also, this will use our Standard analyzer to create terms for the query.
    QueryParser queryParser = new QueryParser("search_content", analyzer);
    //We pass in the title as it is to the query parser.
    // This will create a term query for us on the terms in our title.
    Query query = queryParser.parse("vladyslav");


    //We perform the search and get top 10 search results.
    TopDocs topDocs = indexSearcher.search(query, 10);

    long totalHits = topDocs.totalHits.value;
    System.out.println(String.format("Found %d hits.", totalHits));

    //Print the title field of each matching movie

    ScoreDoc[] results = topDocs.scoreDocs;
    for (ScoreDoc scoreDoc : results) {
      //Returns the id of the document matching the query
      int docId = scoreDoc.doc;
      float score = scoreDoc.score;
      System.out.println(score);
      Document movie = indexSearcher.doc(docId);
      System.out.println(String.format("Found: %s", movie.get("path")));
//      System.out.println(String.format("Found: %s", movie.get("title")));
//      System.out.println(String.format("Found: %s", movie.get("content")));
//      System.out.println(String.format("Found: %s", movie.get("content_norm")));
    }

    indexReader.close();
  }

  @SneakyThrows
  private static void writeToIndex() {
    Directory indexDirectory = new MMapDirectory(Path.of("lucene_index"));
    StandardAnalyzer analyzer = new StandardAnalyzer(new FileReader(STOP_WORDS_PATH.toFile()));

    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

    IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
    var wordDocumentReader = new WordDocumentReader();

    var content = wordDocumentReader.getContent(DOCX_20);

    Document document = new Document();
    document.add(new TextField("title", "20.docx", Field.Store.YES));
    document.add(new TextField("content", content, Field.Store.YES));

    document.add(new TextField("title_norm", "20.docx", Field.Store.NO));
    document.add(new TextField("content_norm", content, Field.Store.NO));

    indexWriter.addDocument(document);

    indexWriter.close();
  }
}