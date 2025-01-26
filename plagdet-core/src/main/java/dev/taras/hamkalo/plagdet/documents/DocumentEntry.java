package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.comparision.MarkingOffsets;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
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
  @ToString.Exclude
  @Builder.Default
  List<DocumentPair> documentPairs = new ArrayList<>();

  @NotNull
  @Builder.Default
  List<SpecialMarking> templateMarking = new ArrayList<>();

  @Nullable
  @ToString.Exclude
  @Builder.Default
  List<SpecialMarking> codeMarking = null;

  public int getId() {
    return 31 * this.fileData.getContent().hashCode() + this.fileData.getPath().hashCode();
  }

  public @NotNull List<SpecialMarking> getCodeMarking() {
    if (this.codeMarking != null) {
      return this.codeMarking;
    }

    final var sentences = this.coreDocument.annotation()
      .get(CoreAnnotations.SentencesAnnotation.class);
    final var codeSentencesIndexes = this.coreDocument.annotation()
      .get(PlagdetAnnotations.CodeSentencesAnnotation.class);

    this.codeMarking = codeSentencesIndexes.stream()
      .map(sentences::get)
      .map(MarkingOffsets::from)
      .map(marking ->
        SpecialMarking.builder()
          .type(SpecialMarkingType.CODE)
          .first(marking)
          .build()
      ).toList();

    return this.codeMarking;
  }

}
