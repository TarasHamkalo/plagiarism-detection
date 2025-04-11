package dev.taras.hamkalo.plagdet.documents;


import edu.stanford.nlp.pipeline.Annotation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
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
