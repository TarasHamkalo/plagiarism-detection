package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.preprocessing.Token;
import dev.taras.hamkalo.plagdet.preprocessing.filter.TokenFilter;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.Transformer;
import dev.taras.hamkalo.plagdet.readers.ContentReadException;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.readers.MetaReader;
import dev.taras.hamkalo.plagdet.readers.MetadataReadException;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentProcessor {

  ContentReader contentReader;

  MetaReader metaReader;

  Transformer transformer;

  TokenFilter tokenFilter;

  StanfordCoreNLP pipeline;

  public DocumentEntry readDocument(String strPath) throws DocumentProcessingException {
    var path = Path.of(strPath).toAbsolutePath();
    DocumentEntry documentEntry = new DocumentEntry(path.toString());

    try {
      documentEntry.setContent(contentReader.getContent(path));
    } catch (ContentReadException e) {
      throw new DocumentProcessingException("Content reading failed for document " + path);
    }

    try {
      documentEntry.setMetadata(metaReader.getMetadata(path));
    } catch (MetadataReadException e) {
      log.warn("Metadata reading failed for document {}: {}", path, e.getMessage());
      documentEntry.setMetadata(Metadata.NONE);
    }

    return documentEntry;
  }

  public DocumentEntry transform(DocumentEntry documentEntry) {
    CoreDocument document = new CoreDocument(documentEntry.getContent());
    pipeline.annotate(document);

    List<Token> tokens = new ArrayList<>();
    for (CoreLabel tok : document.tokens()) {
      Token token = transformer.transform(
        Token.builder()
          .content(tok.word())
          .start(tok.beginPosition())
          .end(tok.endPosition())
          .build()
      );

      if (tokenFilter.validate(token)) {
        tokens.add(token);
      }
    }

    documentEntry.setTokens(tokens);
    return documentEntry;
  }
}
