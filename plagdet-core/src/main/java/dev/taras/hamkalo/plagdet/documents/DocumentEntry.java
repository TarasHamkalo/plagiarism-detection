package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.io.FileData;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntry {

  boolean indexed;

  boolean template;

  FileData fileData;

  @EqualsAndHashCode.Exclude
  CoreDocument coreDocument;

  public String getId() {
    return this.fileData.getSubmitter() + this.fileData.getPath();
  }

  // тут могло б бути декілька файлів як для одної директорії
  // тут має бути сет біграм для фільтрування вступної множини
  // в кор документі має бути таг про речення що співпадають зі шаблоном
}
