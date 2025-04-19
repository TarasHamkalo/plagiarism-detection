package dev.taras.hamkalo.plagdet.indexing.lucene.mapping;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationSerializer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import static org.apache.lucene.document.Field.Store;

public class StanfordAnnotationMapper {

  public static void addPreprocessedContent(
    @NotNull DocumentEntry documentEntry,
    @NotNull Document doc
  ) {
    var annotation = documentEntry.getAnnotation();
    var serializedContent = AnnotationSerializer.serialize(annotation);
    doc.add(
      new TextField(LuceneConstants.PREPROCESSED_CONTENT, serializedContent, Store.YES)
    );
  }

}
