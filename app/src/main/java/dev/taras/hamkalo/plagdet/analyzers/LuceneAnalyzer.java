package dev.taras.hamkalo.plagdet.analyzers;

import dev.taras.hamkalo.plagdet.documents.DocumentProcessingException;
import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.index.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LuceneAnalyzer {

  LuceneIndex index;

  DocumentProcessor documentProcessor;

  public List<DocumentEntry> analyze(DocumentEntry documentEntry) {
    if (documentEntry.getTokens().isEmpty()) {
      log.warn("Not initialized document passed: {}", documentEntry.getContentPath());
      return Collections.emptyList();
    }

    System.out.println("- Lucene analyzer -\n");
    System.out.println("File analyzed: " + documentEntry.getContentPath());
    var retrieved = index.retrieveSimilar(documentEntry);
    if (retrieved.isEmpty()) {
      System.out.println("No documents found to be similar to.");
      return Collections.emptyList();
    }

    System.out.println("Found " + retrieved.size() + " similar documents.");
    System.out.println("Listing (name: score):");

    List<DocumentEntry> results = new ArrayList<>();
    for (var result: retrieved) {
      printResult(result);
      try {
        var path = result.documentEntry().getContentPath();
        var processedDocument = documentProcessor.readDocument(path);
        documentProcessor.transform(processedDocument);
        results.add(processedDocument);
      } catch (DocumentProcessingException e) {
        log.warn(
          "Document processing error for document {}: {}",
          result.documentEntry().getContentPath(),
          e.getMessage()
        );
      }
    }

    System.out.println("\n-      End      -\n");
    return results;
  }

  private void printResult(LuceneIndex.LuceneResult result) {
    var path = Path.of(result.documentEntry().getContentPath()).toAbsolutePath();
    System.out.printf("\t%s: %.2f\n", path, result.score());
  }


}
