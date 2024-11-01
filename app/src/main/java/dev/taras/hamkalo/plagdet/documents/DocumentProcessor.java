package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.preprocessing.Preprocessor;
import dev.taras.hamkalo.plagdet.readers.ContentReadException;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentProcessor {

  @Getter
  DocumentEntry documentEntry;

  ContentReader contentReader;

  Preprocessor preprocessor;

  public DocumentProcessor(String path,
                           ContentReader contentReader,
                           Preprocessor textPreprocessor
  ) {
    this.documentEntry = new DocumentEntry(path);
    this.contentReader = contentReader;
    this.preprocessor = textPreprocessor;
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
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
