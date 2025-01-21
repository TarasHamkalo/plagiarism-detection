package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.indexing.exception.IndexingException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.ContentReader;
import dev.taras.hamkalo.plagdet.io.reader.ContentReaders;
import dev.taras.hamkalo.plagdet.io.reader.MetaReader;
import dev.taras.hamkalo.plagdet.io.reader.MetaReaders;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PairsBuilder {

  public static final int SIMILAR_DOCUMENTS_LIMIT = 10;

  ContentReader contentReader;

  MetaReader metadataReader;

  SubmissionsParser submissionsParser;

  DocumentAnnotator annotator;

  LuceneIndex luceneIndex;

  public PairsBuilder(List<ContentReader> contentReaders,
                      List<MetaReader> metaReaders,
                      DocumentAnnotator annotator,
                      LuceneIndex luceneIndex) {
    this.contentReader = new ContentReaders(contentReaders);
    this.metadataReader = new MetaReaders(metaReaders);
    this.submissionsParser = new SubmissionsParser(contentReaders, metaReaders);
    this.annotator = annotator;
    this.luceneIndex = luceneIndex;
  }

  public List<DocumentPair> build(String inputPath) {
    List<Submission> submissions = this.submissionsParser.parseSubmissions(inputPath);
    annotator.annotateSubmissions(submissions);
    // for each document find by meta and lucene
    var pairs = generatePairs(submissions);
    pairs.addAll(generatePairsByLuceneSimilarity(submissions));
    pairs.addAll(generatePairsByMetadataSimilarity(submissions));
    return pairs;
  }

  private List<DocumentPair> generatePairsByMetadataSimilarity(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        try {
          var creator = document.getFileData().getMetadata().getCreator();
          var similarDocuments = luceneIndex.retrieveByCreator(creator, SIMILAR_DOCUMENTS_LIMIT);
          for (var similar : similarDocuments) {
            var similarDocument = new DocumentEntry(true, similar, null);
            annotator.annotateDocument(document);

            pairs.add(DocumentPair.builder()
              .first(document)
              .second(similarDocument)
              .build()
            );
          }
        } catch (IndexingException e) {
          log.error(e.getMessage());
        }
      }
    }

    return pairs;
  }

  private List<DocumentPair> generatePairsByLuceneSimilarity(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (var submission : submissions) {
      for (var document : submission.getDocuments()) {
        try {
          var similarDocuments = luceneIndex.retrieveSimilar(document, SIMILAR_DOCUMENTS_LIMIT);

          for (var similar : similarDocuments) {
            var similarDocument = new DocumentEntry(true, false, similar.fileData(), null);
            annotator.annotateDocument(document);

            pairs.add(DocumentPair.builder()
              .first(document)
              .second(similarDocument)
              .build()
            );
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
            pairs.add(
              DocumentPair.builder()
                .first(firstDocument)
                .second(secondDocument)
                .build()
            );
          }
        }
      }
    }

    return pairs;
  }


}
