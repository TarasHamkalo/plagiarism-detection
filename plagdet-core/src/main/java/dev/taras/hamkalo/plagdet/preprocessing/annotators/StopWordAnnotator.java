package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StopWordAnnotator implements Annotator {

  public static final String STOP_WORD_RESOURCE ="sk-stopwords.txt";

  CharArraySet stopwordSet;

  public StopWordAnnotator() throws IOException {
    this.stopwordSet = loadStopwordSet();
  }

  private static CharArraySet loadStopwordSet() throws IOException {
    var stopWordsPath = Path.of(
      ClassLoader.getSystemClassLoader().getResource(STOP_WORD_RESOURCE).getPath()
    );

    var stopwords = new FileReader(stopWordsPath.toFile());
    return WordlistLoader.getWordSet(stopwords);
  }

  @Override
  public void annotate(Annotation annotation) {
    log.debug("Stopword tagger started");
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
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.ValidatedSentenceAnnotation.class
    );
  }
}
