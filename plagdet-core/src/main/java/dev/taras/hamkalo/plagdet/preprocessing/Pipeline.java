package dev.taras.hamkalo.plagdet.preprocessing;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public abstract class Pipeline {

  public static final String annotators = "tokenize, word, ssplit, morpho, stopword";

  private static StanfordCoreNLP pipeline;

  public static StanfordCoreNLP getPipeline() {
    if (pipeline == null) {
      var props = new Properties();
      props.put("annotators", annotators);

      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoditaStanfordAdapter"
      );

      props.put(
        "customAnnotatorClass.word",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.WordAnnotator"
      );

      props.put(
        "customAnnotatorClass.stopword",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.StopWordAnnotator"
      );

      pipeline = new StanfordCoreNLP(props);
    }

    return pipeline;
  }
}
