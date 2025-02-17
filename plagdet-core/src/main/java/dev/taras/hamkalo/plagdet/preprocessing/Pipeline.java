package dev.taras.hamkalo.plagdet.preprocessing;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public abstract class Pipeline {

  public static final String annotators = "tokenize, ssplit, dynamicssplit, word, morpho, stopword, code, spell";

  private static StanfordCoreNLP pipeline;

  public static StanfordCoreNLP getPipeline() {
    if (pipeline == null) {
      var props = new Properties();
      props.put("annotators", annotators);
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

      props.put("ssplit.eolonly", "false");
      props.put("ssplit.newlineIsSentenceBreak", "two");

      props.put(
        "customAnnotatorClass.code",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.CodeSegmentAnnotator"
      );

      props.put(
        "customAnnotatorClass.dynamicssplit",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.DynamicSentenceSplitter"
      );

      props.put(
        "customAnnotatorClass.spell",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.SpellingAnnotator"
      );

      pipeline = new StanfordCoreNLP(props);
    }

    return pipeline;
  }

}
