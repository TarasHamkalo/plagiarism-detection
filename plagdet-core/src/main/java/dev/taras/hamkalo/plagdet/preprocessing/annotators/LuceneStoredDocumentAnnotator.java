package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.indexing.lucene.mapping.StanfordAnnotationMapper;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations.*;

/**
 * Annotator used to reload document from lucene stored document.
 * It expects TextAnnotation to be provided on Annotation.
 */
public class LuceneStoredDocumentAnnotator implements Annotator {

  @Override
  public void annotate(Annotation annotation) {
    List<CoreMap> sentences = new ArrayList<>();
    var text = annotation.get(CoreAnnotations.TextAnnotation.class);
    var lines = text.split("\n");
    int linesSize = lines.length;
    if (linesSize > 0) {
      StanfordAnnotationMapper.deserializeArraysListToAnnotation(
        annotation, TemplateSentencesAnnotation.class, lines[0], 2
      );
    }

    if (linesSize > 1) {
      StanfordAnnotationMapper.deserializeArraysListToAnnotation(
        annotation, CodeSegmentsAnnotation.class, lines[0], 2
      );
    }

    for (int i = 2; i < linesSize; i++) {
      sentences.add(StanfordAnnotationMapper.deserialize(lines[i]));
    }

    annotation.set(
      CoreAnnotations.TokensAnnotation.class,
      sentences.stream().flatMap(s -> AnnotationUtils.tokens(s).stream()).toList()
    );
    annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);
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
