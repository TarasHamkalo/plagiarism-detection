package dev.taras.hamkalo.plagdet.documents;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentAnnotator {

  StanfordCoreNLP pipeline;

  public void annotateSubmissions(@NotNull List<Submission> submissions) {
    for (Submission submission : submissions) {
      annotateDocuments(submission.getDocuments());
    }
  }

  public void annotateDocuments(@NotNull List<DocumentEntry> documents) {
    for (DocumentEntry document : documents) {
      CoreDocument coreDocument = new CoreDocument(document.getFileData().getContent());
      pipeline.annotate(coreDocument);
      document.setCoreDocument(coreDocument);
    }
  }

}
