package dev.taras.hamkalo.plagdet.indexing.lucene.analysis;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentAnalyzer extends Analyzer {

  public static final ReuseStrategy NEVER_REUSE = new ReuseStrategy() {
    @Override
    public TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
      return null;
    }

    @Override
    public void setReusableComponents(Analyzer analyzer, String fieldName, TokenStreamComponents components) {
    }
  };

  DocumentEntry document;

  public DocumentAnalyzer() {
    super(NEVER_REUSE);
    this.document = null;
  }

  public void setDocument(DocumentEntry document) {
    log.trace("New document entry set to {}", document.getFileData().getPersistentPath());
    this.document = document;
  }

  public DocumentAnalyzer(DocumentEntry document) {
    super(NEVER_REUSE);
    this.document = document;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    log.trace("Tokens for field: {}", fieldName);
    if (fieldName.equals(LuceneConstants.SEARCH_CONTENT) && document != null) {
      log.trace("Creating new tokenizer hash of annotation is {}", document.getAnnotation().hashCode());
      var tokenizer = new StanfordAdapterTokenizer(document.getAnnotation());
      return new TokenStreamComponents(tokenizer);
    }
    return new TokenStreamComponents(new StandardTokenizer());
  }

}

