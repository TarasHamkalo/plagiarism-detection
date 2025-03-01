package dev.taras.hamkalo.plagdet.documents;

import dev.taras.hamkalo.plagdet.comparision.MarkingOffsets;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntry {

  boolean indexed;

  boolean template;

  boolean hasPassedLuceneSearch;

  @NotNull FileData fileData;

  @Nullable
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  CoreDocument coreDocument;

  @NotNull
  @ToString.Exclude
  @Builder.Default
  @EqualsAndHashCode.Exclude
  Queue<DocumentPair> documentPairs = new PriorityQueue<>();

  @NotNull
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<SpecialMarking> templateMarking = new ArrayList<>();

  @Nullable
  @ToString.Exclude
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<SpecialMarking> codeMarking = null;

  public int getId() {
    return getId(this.fileData);
  }

  public static int getId(@NotNull FileData fileData) {
    return 31 * fileData.getContent().hashCode() + fileData.getPersistentPath().hashCode();
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

  public @NotNull List<SpecialMarking> getMisspelledMarking() {
    return this.coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(isMisspelledToken())
      .map(t ->
        SpecialMarking.builder()
          .type(SpecialMarkingType.MISSPELLED)
          .first(MarkingOffsets.from(t))
          .comments(getCorrection(t))
          .build()
      )
      .toList();
  }

  public @NotNull List<SpecialMarking> getSpoofMarking() {
    return this.coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(isSpoofedToken())
      .map(t ->
        SpecialMarking.builder()
          .type(SpecialMarkingType.SPOOF)
          .first(MarkingOffsets.from(t))
          .comments(getCorrection(t))
          .build()
      )
      .toList();
  }

  private Predicate<CoreLabel> isSpoofedToken() {
    return t -> t.get(PlagdetAnnotations.SpoofedWordAnnotation.class);
  }

  private @NotNull List<String> getCorrection(@NotNull CoreLabel token) {
    var correctedWordAnnotation = token.get(PlagdetAnnotations.CorrectedWordAnnotation.class);
    if (correctedWordAnnotation != null) {
      return List.of(correctedWordAnnotation);
    }

    return Collections.emptyList();
  }

  private static @NotNull Predicate<CoreLabel> isMisspelledToken() {
    return t -> t.get(PlagdetAnnotations.MisspelledAnnotation.class) != null &&
                t.get(PlagdetAnnotations.MisspelledAnnotation.class);
  }

}
