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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MorphoditaStanfordAdapter implements Annotator {

  public static final String MISSPELLED_ONLY = "morpho.misspelledonly";

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

  boolean isMisspelledOnly;

  public MorphoditaStanfordAdapter(String name, Properties props) {
    var booleanProperty = props.get(MISSPELLED_ONLY);
    this.isMisspelledOnly = Boolean.parseBoolean(String.valueOf(booleanProperty));
  }

  private Tagger loadTagger() {
    if (tagger == null) {
      var taggerPath = IoUtils.resourceFromTemp(TAGGER_RESOURCE_PREFIX + SK_TAGGER, SK_TAGGER);
      log.debug("Tagger path is [{}]", taggerPath);
      tagger = Tagger.load(taggerPath.toString());
    }

    return tagger;
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Morpho annotator started");

    final var tagger = this.loadTagger();
    final var morpho = tagger.getMorpho();

    final var guessedSentences = new ArrayList<CoreMap>();
    final var sentences = getSentencesForTagging(annotation);
    for (var sentence : sentences) {
      var tokens = sentence.get(PlagdetAnnotations.WordsAnnotation.class);
      var texts = extractTokenText(tokens);

      Forms forms = new Forms(texts);
      TaggedLemmas lemmas = new TaggedLemmas();
      tagger.tag(forms, lemmas, Morpho.GUESSER);

      var hasGuessedLemma = false;
      for (int i = 0; i < tokens.size(); i++) {
        final var guesserMode = morpho.analyze(forms.get(i), Morpho.GUESSER, new TaggedLemmas());
        final var isGuessed = guesserMode == Morpho.GUESSER;
        hasGuessedLemma |= isGuessed;

        var lemma = morpho.rawLemma(lemmas.get(i).getLemma());
        var tag = remapTag(lemmas.get(i).getTag());

        tokens.get(i).set(PlagdetAnnotations.GuessedLemmaAnnotation.class, isGuessed);
        tokens.get(i).set(CoreAnnotations.LemmaAnnotation.class, lemma);
        tokens.get(i).set(CoreAnnotations.PartOfSpeechAnnotation.class, tag);
      }

      if (hasGuessedLemma) {
        guessedSentences.add(sentence);
      }
    }

    annotation.set(PlagdetAnnotations.GuessedSentencesAnnotation.class, guessedSentences);
  }

  private @NotNull List<String> extractTokenText(@NotNull List<CoreLabel> tokens) {
    return tokens.stream()
      .map(t -> {
        var corrected = t.get(PlagdetAnnotations.CorrectedWordAnnotation.class);
        return corrected != null ? corrected : t.get(PlagdetAnnotations.WordAnnotation.class);
      })
      .map(String::toLowerCase)
      .toList();
  }

  private @NotNull List<CoreMap> getSentencesForTagging(@NotNull Annotation annotation) {
    if (isMisspelledOnly) {
      return annotation.get(PlagdetAnnotations.CorrectedSentencesAnnotation.class);
    }

    return annotation.get(CoreAnnotations.SentencesAnnotation.class);
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
