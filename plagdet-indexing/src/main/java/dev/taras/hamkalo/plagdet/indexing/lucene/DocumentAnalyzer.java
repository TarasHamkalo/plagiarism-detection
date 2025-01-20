package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import static dev.taras.hamkalo.plagdet.indexing.lucene.DocumentFieldNames.SEARCH_CONTENT;


@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentAnalyzer extends Analyzer {

  DocumentEntry document;

  boolean removeStopwords;

  public DocumentAnalyzer(DocumentEntry document, boolean removeStopword) {
    this.document = document;
    this.removeStopwords = removeStopword;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    if (fieldName.equals(SEARCH_CONTENT.toString())) {
      var tokenizer = new StanfordAdapterTokenizer(document.getCoreDocument(), removeStopwords);
      return new TokenStreamComponents(tokenizer);
    }

    return new TokenStreamComponents(new StandardTokenizer());
  }

}

