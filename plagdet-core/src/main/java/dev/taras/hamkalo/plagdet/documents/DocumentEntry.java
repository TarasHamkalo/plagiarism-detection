package dev.taras.hamkalo.plagdet.documents;


import edu.stanford.nlp.pipeline.Annotation;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
public class DocumentEntry {

  boolean isIndexed;

  @NotNull Annotation annotation;

  @NotNull FileData fileData;

  public int getId() {
    return DocumentEntry.getId(fileData);
  }

  public static int getId(@NotNull FileData fileData) {
    return 31 * fileData.getContent().hashCode() + fileData.getPersistentPath().hashCode();
  }

}
