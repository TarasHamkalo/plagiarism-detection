package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import cz.cuni.mff.ufal.morphodita.Forms;
import cz.cuni.mff.ufal.morphodita.Morpho;
import cz.cuni.mff.ufal.morphodita.TaggedLemmas;
import cz.cuni.mff.ufal.morphodita.Tagger;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MorphoditaStanfordAdapter implements Annotator {

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

  Tagger tagger;

  Morpho morpho;

  public MorphoditaStanfordAdapter() {
    this.tagger = this.loadTagger();
    this.morpho = tagger.getMorpho();
  }

  private Tagger loadTagger() {
    if (tagger != null) {
      return tagger;
    }

    var taggerPath = IoUtils.resourceFromTemp(TAGGER_RESOURCE_PREFIX + SK_TAGGER, SK_TAGGER);
    log.debug("Tagger path is " + taggerPath);
    return Tagger.load(taggerPath.toString());
  }

  @Override
  public void annotate(Annotation annotation) {
    final var guessedSentences = new ArrayList<CoreMap>();
    for (var sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var tokens = sentence.get(PlagdetAnnotations.WordsAnnotation.class);
      var texts = tokens.stream()
        .map(t -> t.get(PlagdetAnnotations.WordAnnotation.class))
//        .filter(Objects::nonNull)
        .map(String::toLowerCase)
        .toList();

      Forms forms = new Forms(texts);
      TaggedLemmas lemmas = new TaggedLemmas();
      tagger.tag(forms, lemmas, Morpho.GUESSER);

      var hasGuessedLemma = false;
      for (int i = 0; i < tokens.size(); i++) {
        final var guesserMode = morpho.analyze(forms.get(i), Morpho.GUESSER, new TaggedLemmas());
        final var isGuessed = guesserMode == Morpho.GUESSER;
        hasGuessedLemma |= isGuessed;

        tokens.get(i).set(PlagdetAnnotations.GuessedLemmaAnnotation.class, isGuessed);
        tokens.get(i).set(
          CoreAnnotations.LemmaAnnotation.class, morpho.rawLemma(lemmas.get(i).getLemma())
        );

        tokens.get(i).set(
          CoreAnnotations.PartOfSpeechAnnotation.class, remapTag(lemmas.get(i).getTag())
        );
      }

      if (hasGuessedLemma) {
        guessedSentences.add(sentence);
      }
    }

    annotation.set(PlagdetAnnotations.GuessedSentencesAnnotation.class, guessedSentences);
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
      PlagdetAnnotations.GuessedLemmaAnnotation.class,
      PlagdetAnnotations.GuessedSentencesAnnotation.class
    );
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TextAnnotation.class,
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class,
      PlagdetAnnotations.WordAnnotation.class
    );
  }


}
