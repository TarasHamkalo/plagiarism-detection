package dev.taras.hamkalo.plagdet.analyzers;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessingException;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.index.metadata.MetadataIndex;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MetaAnalyzer {

  MetadataIndex index;

  DocumentProcessor documentProcessor;

  public List<DocumentEntry> analyze(DocumentEntry documentEntry) {
    System.out.println("- Meta analyzer -\n");

    printDocumentMeta(documentEntry.getContentPath(), documentEntry.getMetadata());

    var retrieved = index.retrieveSimilar(documentEntry);
    if (retrieved.isEmpty()) {
      System.out.println("No documents with the same author found.");
      return Collections.emptyList();
    }

    System.out.println("Found " + retrieved.size() + " documents with same author.");
    System.out.println("Listing:");
    for (var document: retrieved) {
      System.out.println("\t" + document.getContentPath());
    }

    List<DocumentEntry> result = new ArrayList<>();
    for (var document : retrieved) {
      try {
        var processedDocument = documentProcessor.readDocument(document.getContentPath());
        documentProcessor.transform(processedDocument);
        result.add(processedDocument);
      } catch (DocumentProcessingException e) {
        log.warn(
          "Document processing error for document {}: {}", document.getContentPath(), e.getMessage()
        );
      }

    }
    System.out.println("\n-      End      -\n");

    return result;
  }

  public void printDocumentMeta(String path, Metadata metadata) {
    System.out.println("File analyzed: " + path);
    System.out.println("\tWas created by: " + metadata.getAuthorName());
    System.out.println("\tFile creation date: " + metadata.getCreationDate());
    System.out.println("\tFile edit time: " + metadata.getTotalEditTime());
  }

}
