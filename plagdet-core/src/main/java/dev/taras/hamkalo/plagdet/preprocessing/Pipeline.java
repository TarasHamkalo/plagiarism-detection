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
      props.put("annotators", "tokenize, ssplit, word, dynamicssplit");
      props.put("ssplit.eolonly", "false");
      props.put("ssplit.newlineIsSentenceBreak", "two");

      props.put(
        "customAnnotatorClass.word",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.WordAnnotator"
      );

      props.put(
        "customAnnotatorClass.dynamicssplit",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.DynamicSentenceSplitter"
      );

      firstStagePipeline = new StanfordCoreNLP(props);
    }

    return firstStagePipeline;
  }

  public static StanfordCoreNLP getSecondStagePipeline() {
    if (secondStagePipeline == null) {
      var props = new Properties();
      props.put("annotators", "word, morpho, stopword, code, spell");
      props.put(
        "customAnnotatorClass.word",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.WordAnnotator"
      );

      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter"
      );

      props.put(
        "customAnnotatorClass.stopword",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.StopWordAnnotator"
      );

      props.put(
        "customAnnotatorClass.code",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.CodeSegmentAnnotator"
      );

      props.put(
        "customAnnotatorClass.spell",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.SpellingAnnotator"
      );

      props.put("enforceRequirements", "false");
      secondStagePipeline = new StanfordCoreNLP(props);
    }

    return secondStagePipeline;
  }

  public static StanfordCoreNLP getPostLemmatizationPipeline() {
    if (postLemmatizationPipeline == null) {
      var props = new Properties();
      props.put("annotators", "morpho");
      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter"
      );

      props.put(
        MorphoditaStanfordAdapter.MISSPELLED_ONLY, "true"
      );

      props.put("enforceRequirements", "false");

      postLemmatizationPipeline = new StanfordCoreNLP(props);
    }

    return postLemmatizationPipeline;
  }

  public static void createPipelines() {
    firstStagePipeline = getFirstStagePipeline();
    secondStagePipeline = getSecondStagePipeline();
    postLemmatizationPipeline = getPostLemmatizationPipeline();
  }

  public static void annotate(@NotNull CoreDocument coreDocument) {
    firstStagePipeline.annotate(coreDocument);
    secondStagePipeline.annotate(coreDocument);
    postLemmatizationPipeline.annotate(coreDocument);
  }
}
