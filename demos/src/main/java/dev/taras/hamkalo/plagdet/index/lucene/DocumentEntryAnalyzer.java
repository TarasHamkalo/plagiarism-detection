package dev.taras.hamkalo.plagdet.index.lucene;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import static dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex.DocumentFieldNames.SEARCH_CONTENT;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentEntryAnalyzer extends Analyzer {

  DocumentEntry documentEntry;

  public DocumentEntryAnalyzer(DocumentEntry documentEntry) {
    this.documentEntry = documentEntry;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    if (fieldName.equals(SEARCH_CONTENT.toString())) {
      var tokenizer = new TokenListTokenizer(documentEntry.getTokens());
      return new TokenStreamComponents(tokenizer);
    }

    return new TokenStreamComponents(new StandardTokenizer());
  }

}

