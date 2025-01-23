package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.io.FileData;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntry {

  boolean indexed;

  boolean template;

  @NotNull FileData fileData;

  @Nullable
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  CoreDocument coreDocument;

  @NotNull
  @Builder.Default
  List<DocumentPair> documentPairs = new ArrayList<>();

  public int getId() {
    return 31 * this.fileData.getContent().hashCode() + this.fileData.getPath().hashCode();
  }

  // тут могло б бути декілька файлів як для одної директорії
  // тут має бути сет біграм для фільтрування вступної множини
  // в кор документі має бути таг про речення що співпадають зі шаблоном
}
