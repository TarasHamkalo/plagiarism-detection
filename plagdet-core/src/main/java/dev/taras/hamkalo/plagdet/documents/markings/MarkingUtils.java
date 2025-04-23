package dev.taras.hamkalo.plagdet.documents.markings;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking.SpecialMarkingType.*;

public class MarkingUtils {
  public static @NotNull List<SpecialMarking> getMisspelledMarking(@NotNull Annotation annotation) {
    var misspelledTokens = AnnotationUtils.tokens(annotation).stream()
      .filter(AnnotationUtils.isMisspelledToken())
      .toList();

    return misspelledTokens.stream()
      .map(t ->
        SpecialMarking.builder()
          .type(MISSPELLED)
          .first(MarkingOffsets.from(t))
          .comments(AnnotationUtils.getCorrection(t))
          .build()
      ).toList();
  }

  public static @NotNull List<SpecialMarking> getCodeMarking(@NotNull Annotation annotation) {
    final var codeSentenceBoundaries = annotation
      .get(PlagdetAnnotations.CodeSegmentsAnnotation.class);

    if (codeSentenceBoundaries == null) {
      return Collections.emptyList();
    }

    return codeSentenceBoundaries.stream()
      .map(b -> new MarkingOffsets(b[0], b[1]))
      .map(marking ->
        SpecialMarking.builder()
          .type(CODE)
          .first(marking)
          .build()
      ).toList();
  }

  public static @NotNull List<SpecialMarking> getTemplateMarkings(@NotNull Annotation annotation) {
    final var templateOffsets = annotation
      .get(PlagdetAnnotations.TemplateSentencesAnnotation.class);

    if (templateOffsets == null) {
      return Collections.emptyList();
    }

    return templateOffsets.stream()
      .map(b -> new MarkingOffsets(b[0], b[1]))
      .map(marking ->
        SpecialMarking.builder()
          .type(TEMPLATE)
          .first(marking)
          .build()
      ).toList();
  }

  public static @NotNull List<SpecialMarking> getMarkings(@NotNull Annotation annotation) {
    var markings = new ArrayList<SpecialMarking>();
    markings.addAll(getTemplateMarkings(annotation));
    markings.addAll(getCodeMarking(annotation));
    markings.addAll(getMisspelledMarking(annotation));

    return markings;
  }
}
