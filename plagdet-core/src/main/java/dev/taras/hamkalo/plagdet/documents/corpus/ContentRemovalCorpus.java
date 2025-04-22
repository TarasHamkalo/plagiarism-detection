package dev.taras.hamkalo.plagdet.documents.corpus;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.io.filedata.FileDataSource;
import dev.taras.hamkalo.plagdet.io.report.BufferedReportWriter;
import edu.stanford.nlp.pipeline.Annotation;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContentRemovalCorpus extends BaseCorpus {

  @NotNull BufferedReportWriter writer;

  public ContentRemovalCorpus(
    @NotNull BufferedReportWriter writer,
    @NotNull FileDataSource fileDataSource
  ) {
    super(fileDataSource);
    this.writer = writer;
  }

  @Override
  protected @NotNull DocumentEntry createDocumentEntry(
    @NotNull FileData next,
    @NotNull Annotation annotation
  ) {
    var entry = super.createDocumentEntry(next, annotation);
    writer.flushDocumentContent(entry);
    entry.getFileData().setContent(null);
    return entry;
  }
}
