package dev.taras.hamkalo.plagdet.preprocessing.annotators;


import edu.stanford.nlp.ling.CoreLabel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.Hunspell;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpellingAnnotator implements Annotator {

  public static final String SPELL_RESOURCE_PREFIX = "spell/";

  public static final String DICT_RESOURCE = "sk_SK.dic";

  public static final String AFFIX_RESOURCE = "sk_SK.aff";

  public static final String SPELL_DIRECTORY_PREFIX = "spell_directory_";

  Directory spellDirectory;

  Hunspell hunspell;

  public SpellingAnnotator() {
    this.spellDirectory = this.initSpellDirectory();

    this.hunspell = this.loadHunspell();
  }

  @SneakyThrows
  private Directory initSpellDirectory() {
    var spellDirectory = Files.createTempDirectory(SPELL_DIRECTORY_PREFIX);
    return new MMapDirectory(spellDirectory);
  }

  private Hunspell loadHunspell() {

    var dictInputStream = ClassLoader.getSystemClassLoader()
      .getResourceAsStream(SPELL_RESOURCE_PREFIX + DICT_RESOURCE);

    var affixInputStream = ClassLoader.getSystemClassLoader()
      .getResourceAsStream(SPELL_RESOURCE_PREFIX + AFFIX_RESOURCE);
    try (dictInputStream; affixInputStream) {
      Dictionary dictionary = new Dictionary(
        this.spellDirectory, "spell", affixInputStream, dictInputStream
      );
      return new Hunspell(dictionary);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void annotate(Annotation annotation) {

    for (var token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
      var isMisspelled = false;
      if (token.get(PlagdetAnnotations.WordAnnotation.class) != null) {
        isMisspelled = !this.hunspell.spell(token.word());
        if (isMisspelled) {
          var suggestions = this.hunspell.suggest(token.word());
          if (!suggestions.isEmpty()) {
            var pick = pickSuggestion(token, suggestions);
            log.error("[Lemma: {}] [Misspelled: {}] [Chosen: {}] [Options: {}]", token.lemma(), token.word(), pick, suggestions);
          }
        }
      }
      token.set(PlagdetAnnotations.MisspelledAnnotation.class, isMisspelled);
    }
  }

  private String pickSuggestion(CoreLabel token, List<String> suggestions) {
    int min = 0;
    double minDistance = Double.MAX_VALUE;
    for (int i = 0; i < suggestions.size(); i++) {
      var suggestion = suggestions.get(i);
      double suggestionDistance = distance(token, suggestion);
      if (suggestionDistance < minDistance) {
        min = i;
        minDistance = suggestionDistance;
      }
    }

    return suggestions.get(min);
  }

  private double distance(CoreLabel token, String suggestion) {
    var noDiaDistance = LevenshteinDistance.getDefaultInstance().apply(
      StringUtils.stripAccents(token.word().toLowerCase()),
      StringUtils.stripAccents(suggestion.toLowerCase())
    );

    if (noDiaDistance <= 1) {
      return 0;
    }

    var diaDistance = LevenshteinDistance.getDefaultInstance().apply(
      token.word().toLowerCase(), suggestion.toLowerCase()
    );

    var mmwMultiplier = suggestion.contains(" ") ? 2 : 1;

    return mmwMultiplier * (0.8 * noDiaDistance + diaDistance);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.MisspelledAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.SentencesAnnotation.class,
      CoreAnnotations.LemmaAnnotation.class,
      PlagdetAnnotations.WordsAnnotation.class,
      PlagdetAnnotations.ValidatedSentenceAnnotation.class
    );
  }

  @Override
  @SneakyThrows
  public void unmount() {
    this.spellDirectory.close();
  }

}
