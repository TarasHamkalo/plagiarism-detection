package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import cz.cuni.mff.ufal.morphodita.Forms;
import cz.cuni.mff.ufal.morphodita.Morpho;
import cz.cuni.mff.ufal.morphodita.TaggedLemmas;
import cz.cuni.mff.ufal.morphodita.Tagger;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MorphoDiTaAnalysisAnnotator implements Annotator {

  public static final String TAGGER_RESOURCE_PREFIX = "models/";

  public static final String SK_TAGGER = "slovak-morfflex-pdt-170914-pos_only.tagger";

  public static final Map<Pattern, String> posMapping = Map.ofEntries(
    Map.entry(Pattern.compile("NN"), "Nn"),
    Map.entry(Pattern.compile("A.*"), "Aj"),
    Map.entry(Pattern.compile("P.*"), "Pn"),
    Map.entry(Pattern.compile("C.*"), "Nm"),
    Map.entry(Pattern.compile("V.*"), "Vb"),
    Map.entry(Pattern.compile("D.*"), "Av"),
    Map.entry(Pattern.compile("R.*"), "Pp"),
    Map.entry(Pattern.compile("J.*"), "Cj"),
    Map.entry(Pattern.compile("T.*"), "Pt"),
    Map.entry(Pattern.compile("B.*"), "Ab"),
    Map.entry(Pattern.compile("Q.*"), "Sy")
  );

  private static Tagger tagger;

  public MorphoDiTaAnalysisAnnotator() {
    loadTagger();
  }

  private static synchronized void loadTagger() {
    if (tagger == null) {
      var taggerPath = IoUtils.resourceFromTemp(TAGGER_RESOURCE_PREFIX + SK_TAGGER, SK_TAGGER);
      log.debug("Tagger path is [{}]", taggerPath);
      tagger = Tagger.load(taggerPath.toString());
    }
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Morpho annotator started");
    final var morpho = tagger.getMorpho();
    for (var sentence : AnnotationUtils.sentences(annotation)) {
      var coreLabels = AnnotationUtils.wordTokens(sentence);
      var tokenStrings = new ArrayList<String>();
      var isAnalyzed = true;
      for (var coreLabel : coreLabels) {
        isAnalyzed &= coreLabel.lemma() != null;
        tokenStrings.add(coreLabel.word());
      }

      if (isAnalyzed) {
        log.warn("Skipping sentence {}", sentence.get(CoreAnnotations.TextAnnotation.class));
        continue;
      }

      Forms forms = new Forms(tokenStrings);
      TaggedLemmas lemmas = new TaggedLemmas();
      tagger.tag(forms, lemmas, Morpho.GUESSER);
      for (int i = 0; i < coreLabels.size(); i++) {
        var isGuessed = isGuessed(morpho, forms.get(i));
        coreLabels.get(i).set(PlagdetAnnotations.GuessedLemmaAnnotation.class, isGuessed);
        coreLabels.get(i).setLemma(morpho.rawLemma(lemmas.get(i).getLemma()));
        coreLabels.get(i).setTag(remapTag(lemmas.get(i).getTag()));
      }
    }
  }

  private static boolean isGuessed(Morpho morpho, String word) {
    return Morpho.GUESSER == morpho.analyze(word, Morpho.GUESSER, new TaggedLemmas());
  }

  private String remapTag(String tag) {
    for (var entry : posMapping.entrySet()) {
      if (entry.getKey().matcher(tag).matches()) {
        return entry.getValue();
      }
    }
    return tag;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(
      CoreAnnotations.PartOfSpeechAnnotation.class,
      CoreAnnotations.LemmaAnnotation.class,
      PlagdetAnnotations.GuessedLemmaAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TextAnnotation.class,
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class
    );
  }

}
