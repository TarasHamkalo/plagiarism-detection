package dev.taras.hamkalo.plagdet.documents;


import edu.stanford.nlp.pipeline.Annotation;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
public class DocumentEntry {

  int id;

  @NotNull Annotation annotation;

  @NotNull FileData fileData;

  public static class DocumentEntryBuilder {

    public DocumentEntry build() {
      int calculatedId = getId(fileData);
      return new DocumentEntry(calculatedId, annotation, fileData);
    }

  }

  public static int getId(@NotNull FileData fileData) {
    return 31 * fileData.getContent().hashCode() + fileData.getPersistentPath().hashCode();
  }

}
