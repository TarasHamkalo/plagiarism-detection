package dev.taras.hamkalo.plagdet.documents.markings;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking.SpecialMarkingType.*;

public class MarkingUtils {
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

}
