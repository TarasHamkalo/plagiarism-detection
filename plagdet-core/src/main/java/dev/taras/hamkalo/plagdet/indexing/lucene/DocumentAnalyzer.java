package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentAnalyzer extends Analyzer {

  DocumentEntry document;

  boolean removeStopwords;

  public DocumentAnalyzer(DocumentEntry document, boolean removeStopword) {
    super(PER_FIELD_REUSE_STRATEGY);
    this.document = document;
    this.removeStopwords = removeStopword;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    log.trace("Tokens for field: {}", fieldName);
    if (fieldName.equals(LuceneConstants.SEARCH_CONTENT)) {
      var tokenizer = new StanfordAdapterTokenizer(document.getCoreDocument(), removeStopwords);
      return new TokenStreamComponents(tokenizer);
    }
    return new TokenStreamComponents(new StandardTokenizer());
  }

}

