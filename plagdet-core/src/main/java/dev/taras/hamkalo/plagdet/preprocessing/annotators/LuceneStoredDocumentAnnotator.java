package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationSerializer;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;

import java.util.Set;

/**
 * Annotator used to reload document from lucene stored document.
 * It expects TextAnnotation to be provided on Annotation.
 */
public class LuceneStoredDocumentAnnotator implements Annotator {

  @Override
  public void annotate(Annotation annotation) {
    Annotation source =
      AnnotationSerializer.deserialize(annotation.get(CoreAnnotations.TextAnnotation.class));

    AnnotationUtils.copy(CoreAnnotations.SentencesAnnotation.class, annotation, source);
    AnnotationUtils.copy(CoreAnnotations.TokensAnnotation.class, annotation, source);
    AnnotationUtils.copy(PlagdetAnnotations.CodeSegmentsAnnotation.class, annotation, source);
    AnnotationUtils.copy(PlagdetAnnotations.TemplateSentencesAnnotation.class, annotation, source);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(CoreAnnotations.SentencesAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of();
  }
}
