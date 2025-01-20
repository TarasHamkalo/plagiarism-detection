package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.io.reader.ContentReader;
import dev.taras.hamkalo.plagdet.io.reader.ContentReaders;
import dev.taras.hamkalo.plagdet.io.reader.MetaReader;
import dev.taras.hamkalo.plagdet.io.reader.MetaReaders;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PairsBuilder {

  ContentReader contentReader;

  MetaReader metadataReader;

  SubmissionsParser submissionsParser;

  DocumentAnnotator annotator;

  public PairsBuilder(List<ContentReader> contentReaders,
                                List<MetaReader> metaReaders,
                                DocumentAnnotator annotator) {
    this.contentReader = new ContentReaders(contentReaders);
    this.metadataReader = new MetaReaders(metaReaders);
    this.submissionsParser = new SubmissionsParser(contentReaders, metaReaders);
    this.annotator = annotator;
  }

  public List<DocumentPair> build(String inputPath) {
    List<Submission> submissions = this.submissionsParser.parseSubmissions(inputPath);
    annotator.annotateDocuments(submissions);
    // for each document find by meta and lucene
    return generatePairs(submissions);
  }

  private static List<DocumentPair> generatePairs(List<Submission> submissions) {
    List<DocumentPair> pairs = new ArrayList<>();
    for (int i = 0; i < submissions.size(); i++) {
      Submission first = submissions.get(i);
      for (var firstDocument : first.getDocuments()) {
        for (int j = i + 1; j < submissions.size(); j++) {
          Submission second = submissions.get(j);
          for (var secondDocument : second.getDocuments()) {
            pairs.add(new DocumentPair(firstDocument, secondDocument, new HashSet<>()));
          }
        }
      }
    }

    return pairs;
  }


}
