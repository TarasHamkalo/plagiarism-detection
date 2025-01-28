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

import java.util.*;

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
  Queue<DocumentPair> documentPairs = new PriorityQueue<>(Comparator.reverseOrder());

  @NotNull
  @Builder.Default
  List<SpecialMarking> templateMarking = new ArrayList<>();

  @Nullable
  @ToString.Exclude
  @Builder.Default
  List<SpecialMarking> codeMarking = null;

  public int getId() {
    return getId(this.fileData);
  }

  public static int getId(@NotNull FileData fileData) {
    return 31 * fileData.getContent().hashCode() + fileData.getPath().hashCode();
  }

  public boolean isComparedWithTemplate() {
    return this.template || !this.templateMarking.isEmpty();
  }

  public boolean isAnnotated() {
    return this.coreDocument != null && this.coreDocument.annotation().size() != 0;
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
