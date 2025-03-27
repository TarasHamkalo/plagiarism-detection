package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public abstract class Pipeline {

  private static StanfordCoreNLP firstStagePipeline;

  private static StanfordCoreNLP secondStagePipeline;

  private static StanfordCoreNLP postLemmatizationPipeline;

  public static StanfordCoreNLP getFirstStagePipeline() {
    if (firstStagePipeline == null) {

      var props = new Properties();
      props.put("annotators", "tokenize, ssplit, morpho, spell");//, code, spell");

      props.put("ssplit.eolonly", "false");
      props.put("ssplit.newlineIsSentenceBreak", "two");

      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter"
      );


      props.put(
        "customAnnotatorClass.spell",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.SpellingAnnotator"
      );

      props.put("enforceRequirements", "false");
      firstStagePipeline = new StanfordCoreNLP(props);
    }

    return firstStagePipeline;
  }

  public static StanfordCoreNLP getSecondStagePipeline() {
    if (secondStagePipeline == null) {
      var props = new Properties();
      props.put("annotators", "morpho, code, dynamicssplit, word, stopword");
      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter"
      );

      props.put(
        MorphoditaStanfordAdapter.MISSPELLED_ONLY, "true"
      );


      props.put(
        "customAnnotatorClass.code",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.CodeSegmentAnnotator"
      );

      props.put(
        "customAnnotatorClass.stopword",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.StopWordAnnotator"
      );

      props.put(
        "customAnnotatorClass.word",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.WordFilteringAnnotator"
      );

      props.put(
        "customAnnotatorClass.dynamicssplit",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.SentenceLengthSplitterAnnotator"
      );

      props.put("enforceRequirements", "false");
      secondStagePipeline = new StanfordCoreNLP(props);
    }

    return secondStagePipeline;
  }

  public static void init() {
    firstStagePipeline = getFirstStagePipeline();
    secondStagePipeline = getSecondStagePipeline();
  }

  public static void annotate(@NotNull CoreDocument coreDocument) {
    firstStagePipeline.annotate(coreDocument);
    secondStagePipeline.annotate(coreDocument);
  }
}
