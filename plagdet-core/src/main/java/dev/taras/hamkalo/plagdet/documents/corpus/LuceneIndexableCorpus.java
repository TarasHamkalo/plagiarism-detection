package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import edu.stanford.nlp.pipeline.Annotation;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LuceneIndexableCorpus extends BaseIndexableCorpus {

  public LuceneIndexableCorpus(
    @NotNull Embeddings embeddings,
    @NotNull FileDataSource fileDataSource,
    @NotNull Integer targetSentenceCount,
    double sizeRatioThreshold
  ) {
    super(embeddings, fileDataSource, targetSentenceCount, sizeRatioThreshold);
  }

  @Override
  protected @NotNull Annotation createAnnotation(@NotNull FileData next) {
    if (next.getPreprocessedContent() != null) {
      var annotation = Pipeline.luceneAnnotate(next.getPreprocessedContent());
      next.setPreprocessedContent(null);
      return annotation;
    } else {
      return Pipeline.annotate(next.getContent());
    }
  }
}
