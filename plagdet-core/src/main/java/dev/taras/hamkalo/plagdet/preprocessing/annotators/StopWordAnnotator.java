package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import java.io.FileReader;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StopWordAnnotator implements Annotator {

  public static final String STOP_WORD_RESOURCE ="sk-stopwords.txt";

  private static final CharArraySet stopwordSet = loadStopwordSet();

  @SneakyThrows
  private static CharArraySet loadStopwordSet() {
    var stopWordsPath = IoUtils.resourceFromTemp(STOP_WORD_RESOURCE, STOP_WORD_RESOURCE);
    var stopwords = new FileReader(stopWordsPath.toFile());
    var stopwordSet = WordlistLoader.getWordSet(stopwords);
    return stopwordSet;
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Stopword tagger started");
    for (var coreLabel : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
      var isStopword = false;
      if (coreLabel.get(PlagdetAnnotations.WordAnnotation.class) != null) {
          isStopword = stopwordSet.contains(coreLabel.word().toLowerCase());
      }
      coreLabel.set(PlagdetAnnotations.StopWordAnnotation.class, isStopword);
    }

  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      PlagdetAnnotations.StopWordAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      PlagdetAnnotations.WordAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class
    );
  }
}
