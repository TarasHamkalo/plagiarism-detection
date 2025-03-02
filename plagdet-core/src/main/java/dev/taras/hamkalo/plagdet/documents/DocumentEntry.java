package dev.taras.hamkalo.plagdet.documents;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import dev.taras.hamkalo.plagdet.comparision.MarkingOffsets;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;
import dev.taras.hamkalo.plagdet.io.FileData;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // used for json to object mapping
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntry {

  boolean indexed;

  boolean template;

  boolean luceneSearchPassed;

  Integer id;

  @NotNull
  FileData fileData;

  @Nullable
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  CoreDocument coreDocument;

  @NotNull
  @ToString.Exclude
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<String> pairIds = new ArrayList<>();

  @NotNull
  @ToString.Exclude
  @Builder.Default
  @JsonIgnore
  @EqualsAndHashCode.Exclude
  Queue<DocumentPair> documentPairs = new PriorityQueue<>();

  @NotNull
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<Integer> indexCache = new ArrayList<>();

  @NotNull
  @JsonIgnore
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<SpecialMarking> templateMarking = new ArrayList<>();

  @Nullable
  @JsonIgnore
  @ToString.Exclude
  @Builder.Default
  @EqualsAndHashCode.Exclude
  List<SpecialMarking> codeMarking = null;

  @JsonGetter("id")
  public Integer getId() {
    if (id != null) {
      return id;
    }

    return id = getId(this.fileData);
  }

  @JsonGetter("maxSimilarity")
  public double maxSimilarity() {
    return documentPairs.stream()
      .mapToDouble(p -> p.getMaxScore())
      .max()
      .orElse(0);
  }

  @JsonGetter("pairIds")
  public List<String> getDocumentPairIds() {
    return documentPairs.stream()
      .map(p -> p.getId())
      .toList();
  }


  @JsonGetter("markings")
  public @NotNull List<SpecialMarking> getMarkings() {
    var markings = new ArrayList<>(getTemplateMarking());
    markings.addAll(getCodeMarking());
    markings.addAll(getMisspelledMarking());
    markings.addAll(getSpoofMarking());
    return markings;
  }

  @JsonSetter("markings")
  public void setMarkings(List<SpecialMarking> markings) {
    this.templateMarking = markings.stream()
      .filter(m -> SpecialMarkingType.TEMPLATE.equals(m.getType()))
      .toList();
  }


  public static int getId(@NotNull FileData fileData) {
    return 31 * fileData.getContent().hashCode() + fileData.getPersistentPath().hashCode();
  }

  @JsonIgnore
  public boolean isAnnotated() {
    return this.coreDocument != null && this.coreDocument.annotation().size() != 0;
  }

  @JsonIgnore
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

    return codeMarking;
  }

  @JsonIgnore
  public @NotNull List<SpecialMarking> getMisspelledMarking() {
    return coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(AnnotationUtils.isMisspelledToken())
      .map(t ->
        SpecialMarking.builder()
          .type(SpecialMarkingType.MISSPELLED)
          .first(MarkingOffsets.from(t))
          .comments(AnnotationUtils.getCorrection(t))
          .build()
      )
      .toList();
  }

  @JsonIgnore
  public @NotNull List<SpecialMarking> getSpoofMarking() {
    return coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(AnnotationUtils.isSpoofedToken())
      .map(t ->
        SpecialMarking.builder()
          .type(SpecialMarkingType.SPOOF)
          .first(MarkingOffsets.from(t))
          .comments(AnnotationUtils.getCorrection(t))
          .build()
      )
      .toList();
  }

}
