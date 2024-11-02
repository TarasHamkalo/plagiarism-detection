package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.filter.TokenFilter;
import dev.taras.hamkalo.plagdet.readers.ContentReadException;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.preprocessing.Token;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.Transformer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentProcessor {

  @Getter
  DocumentEntry documentEntry;

  ContentReader contentReader;

  Preprocessor preprocessor;

  Transformer transformer;

  TokenFilter tokenFilter;

  StanfordCoreNLP pipeline;

  public DocumentProcessor(String path,
                           ContentReader contentReader,
                           Preprocessor textPreprocessor,
                           Transformer transformer,
                           TokenFilter tokenFilter,
                           StanfordCoreNLP pipeline
  ) {
    this.documentEntry = new DocumentEntry(path);
    this.contentReader = contentReader;
    this.preprocessor = textPreprocessor;
    this.transformer = transformer;
    this.tokenFilter = tokenFilter;
    this.pipeline = pipeline;
  }

  public DocumentEntry preprocess() throws DocumentProcessingException {
    try {
      var rawContent = contentReader.getContent(documentEntry.getPath());
      documentEntry.setContent(rawContent);
    } catch (ContentReadException e) {
      throw new DocumentProcessingException(
        String.format("Reading failed for document %s", documentEntry.getPath().toString())
      );
    }

    // TODO: catch exceptions
    var normalizedContent = preprocessor.process(documentEntry.getContent());
    documentEntry.setNormalizedContent(normalizedContent);

    return documentEntry;
  }

  public DocumentEntry transform() {
    var document = new CoreDocument(documentEntry.getNormalizedContent());
    pipeline.annotate(document);
    List<Token> tokens = new ArrayList<>();
    for (CoreLabel tok : document.tokens()) {
      var token = transformer.transform(
        new Token(tok.word(), null, tok.beginPosition(), tok.endPosition())
      );

      if (tokenFilter.validate(token)) {
        tokens.add(token);
      }
    }

    documentEntry.setTokens(tokens);

    return documentEntry;
  }

}
