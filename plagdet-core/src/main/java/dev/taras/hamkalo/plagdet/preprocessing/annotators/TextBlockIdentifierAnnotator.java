package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Annotator used to populate each sentence map with TextBlockId annotation based on
 * TextBoundaries annotation.
 */
public class TextBlockIdentifierAnnotator implements Annotator {

  @Override
  public void annotate(Annotation annotation) {
    List<Integer> textBoundaries = annotation.get(PlagdetAnnotations.TextBlockBoundaries.class);
    if (textBoundaries == null) {
      return;
    }

    Integer maxId = annotation.get(PlagdetAnnotations.TextBlockCounter.class);
    maxId = maxId == null ? 0 : maxId + 1;
    maxId = assignTextBlockIdentifiers(annotation, textBoundaries, maxId);
    annotation.set(PlagdetAnnotations.TextBlockCounter.class, maxId);
  }

  private Integer assignTextBlockIdentifiers(
    @NotNull Annotation annotation,
    @NotNull List<Integer> textBoundaries,
    int maxId
  ) {
    boolean isFirstAssignment = maxId == 0;
    var sentences = AnnotationUtils.sentences(annotation);
    if (sentences.isEmpty()) {
      return maxId;
    }

    int i = 0;
    int j = 0;

    while (j < textBoundaries.size() && i <= textBoundaries.get(j)) {
      int textBoundaryBlockId = AnnotationUtils.textBlockId(sentences.get(textBoundaries.get(j)));
      if (textBoundaryBlockId != -1) {
        int currentBlockId = AnnotationUtils.textBlockId(sentences.get(i));
        while (currentBlockId != textBoundaryBlockId) {
          i++;
          if (i < sentences.size()) {
            currentBlockId = AnnotationUtils.textBlockId(sentences.get(i));
          } else {
            break;
          }
        }

        if (i >= sentences.size()) {
          break;
        }
      }

      sentences.get(i).set(PlagdetAnnotations.TextBlockId.class, maxId);
      if (i == textBoundaries.get(j)) {
        maxId++;
        j++;
      }

      i++;
    }

    while (isFirstAssignment && i < sentences.size()) {
      sentences.get(i).set(PlagdetAnnotations.TextBlockId.class, maxId);
      i++;
    }
    maxId++;

    return maxId;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.TextBlockId.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of();
  }
}
