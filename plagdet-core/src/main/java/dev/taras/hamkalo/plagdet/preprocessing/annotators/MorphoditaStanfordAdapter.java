package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import cz.cuni.mff.ufal.morphodita.Forms;
import cz.cuni.mff.ufal.morphodita.Morpho;
import cz.cuni.mff.ufal.morphodita.TaggedLemmas;
import cz.cuni.mff.ufal.morphodita.Tagger;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MorphoditaStanfordAdapter implements Annotator {

  public static final String SK_MORPHODITA_TAGGER_RESOURCE =
    "models/slovak-morfflex-pdt-170914-pos_only.tagger";

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

    var taggerPath = ClassLoader.getSystemClassLoader()
      .getResource(SK_MORPHODITA_TAGGER_RESOURCE).getPath();
    System.out.println(taggerPath);
    return Tagger.load(taggerPath);
  }

  @Override
  public void annotate(Annotation annotation) {
    for (var sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      var tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
      var texts = tokens.stream()
        .map(CoreLabel::originalText)
        .map(String::toLowerCase)
        .toList();

      Forms forms = new Forms(texts);
      TaggedLemmas lemmas = new TaggedLemmas();
      tagger.tag(forms, lemmas, Morpho.GUESSER);

      for (int i = 0; i < tokens.size(); i++) {
        tokens.get(i).set(
          CoreAnnotations.LemmaAnnotation.class, morpho.rawLemma(lemmas.get(i).getLemma())
        );
        tokens.get(i).set(
          CoreAnnotations.PartOfSpeechAnnotation.class, remapTag(lemmas.get(i).getTag())
        );
      }
    }
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
      CoreAnnotations.LemmaAnnotation.class
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
