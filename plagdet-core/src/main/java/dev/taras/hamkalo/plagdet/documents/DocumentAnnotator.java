package dev.taras.hamkalo.plagdet.documents;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentAnnotator {

  StanfordCoreNLP pipeline;

  public void annotateSubmissions(List<Submission> submissions) {
    for (Submission submission : submissions) {
      for (var document : submission.getDocuments()) {
        annotateDocument(document);
      }
    }
  }

  public void annotateDocument(DocumentEntry document) {
    CoreDocument coreDocument = new CoreDocument(document.getFileData().getContent());
    pipeline.annotate(coreDocument);
    document.setCoreDocument(coreDocument);
  }

}
