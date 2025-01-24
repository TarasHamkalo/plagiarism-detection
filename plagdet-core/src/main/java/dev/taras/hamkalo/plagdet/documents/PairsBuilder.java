package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.io.reader.ContentReader;
import dev.taras.hamkalo.plagdet.io.reader.ContentReaders;
import dev.taras.hamkalo.plagdet.io.reader.MetaReader;
import dev.taras.hamkalo.plagdet.io.reader.MetaReaders;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PairsBuilder {

  public static final int SIMILAR_DOCUMENTS_LIMIT = 1;

  ContentReader contentReader;

  MetaReader metadataReader;

  DocumentAnnotator annotator;

  LuceneIndex luceneIndex;

  List<DocumentEntry> retrievedIndexedDocuments;

  public PairsBuilder(List<ContentReader> contentReaders,
                      List<MetaReader> metaReaders,
                      DocumentAnnotator annotator,
                      LuceneIndex luceneIndex) {
    this.contentReader = new ContentReaders(contentReaders);
    this.metadataReader = new MetaReaders(metaReaders);
    this.annotator = annotator;
    this.luceneIndex = luceneIndex;
    this.retrievedIndexedDocuments = new ArrayList<>();
  }

  public List<DocumentPair> build(List<Submission> submissions) {
    annotator.annotateSubmissions(submissions);
    // for each document find by meta and lucene
    var pairs = generatePairs(submissions);

//    pairs.addAll(generatePairsByLuceneSimilarity(submissions));
//    pairs.addAll(generatePairsByMetadataSimilarity(submissions));
    return pairs;
  }

  private List<DocumentPair> generatePairsByMetadataSimilarity(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        try {
          var creator = document.getFileData().getMetadata().getCreator();
          if (creator == null) {
            continue;
          }

          var similarDocumentFileData = luceneIndex.retrieveByCreator(creator, SIMILAR_DOCUMENTS_LIMIT);
          for (var similarFileData : similarDocumentFileData) {
            buildAndAnnotate(document, similarFileData, pairs);
          }
        } catch (IndexingException e) {
          log.error(e.getMessage());
        }
      }
    }

    return pairs;
  }

  private void buildAndAnnotate(DocumentEntry document,
                                FileData similarFileData,
                                List<DocumentPair> pairs) {
    var similarDocument = DocumentEntry.builder()
      .indexed(true)
      .fileData(similarFileData)
      .build();
    this.retrievedIndexedDocuments.add(similarDocument);
    annotator.annotateDocument(similarDocument);
    var pair = DocumentPair.builder()
      .first(document)
      .second(similarDocument)
      .build();
    pairs.add(pair);
    document.getDocumentPairs().add(pair);
  }

  private List<DocumentPair> generatePairsByLuceneSimilarity(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        try {
          var similarDocuments = luceneIndex.retrieveSimilar(document, SIMILAR_DOCUMENTS_LIMIT);
          for (var similar : similarDocuments) {
            buildAndAnnotate(document, similar.fileData(), pairs);
          }

        } catch (IndexingException e) {
          log.error(e.getMessage());
        }
      }
    }

    return pairs;
  }

  private List<DocumentPair> generatePairs(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (int i = 0; i < submissions.size(); i++) {
      Submission first = submissions.get(i);
      for (var firstDocument : first.getDocuments()) {
        for (int j = i + 1; j < submissions.size(); j++) {
          Submission second = submissions.get(j);
          for (var secondDocument : second.getDocuments()) {
            var pair = DocumentPair.builder()
              .first(firstDocument)
              .second(secondDocument)
              .build();
            pairs.add(pair);
            firstDocument.getDocumentPairs().add(pair);
            secondDocument.getDocumentPairs().add(pair);
          }
        }
      }
    }

    return pairs;
  }


}
