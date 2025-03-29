package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class Pipeline {

  private static StanfordCoreNLP firstStagePipeline;

  private static StanfordCoreNLP secondStagePipeline;

  public static StanfordCoreNLP getFirstStagePipeline() {
    if (firstStagePipeline == null) {

      var props = new Properties();
      props.put("annotators", "tokenize, ssplit, morpho, code, dynamicssplit, word, stopword, spell");//, code, spell");


      props.put(
        "customAnnotatorClass.code",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.CodeSegmentAnnotator"
      );

      props.put("ssplit.eolonly", "false");
      props.put("ssplit.newlineIsSentenceBreak", "two");

      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoDiTaAnalysisAnnotator"
      );

      props.put(
        "customAnnotatorClass.spell",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.SpellingAnnotator"
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
      firstStagePipeline = new StanfordCoreNLP(props);
    }

    return firstStagePipeline;
  }

  public static StanfordCoreNLP getSecondStagePipeline() {
    if (secondStagePipeline == null) {
      var props = new Properties();

      props.put("annotators", "morpho");
      props.put(
        "customAnnotatorClass.morpho",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.MorphoDiTaAnalysisAnnotator"
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

  public static Annotation annotate(@NotNull DocumentEntry documentEntry) {
    CoreDocument coreDocument = new CoreDocument(documentEntry.getFileData().getContent());
    coreDocument.sentences();
    firstStagePipeline.annotate(coreDocument);
    secondStagePipeline.annotate(coreDocument);

    // spell -> misspelled, tag, pos, code sentences, spoofed
    Annotation dest = new Annotation("");
    dest.set(CoreAnnotations.SentencesAnnotation.class, new ArrayList<>());

    for (var sentence : AnnotationUtils.sentences(coreDocument.annotation())) {
      sentence.remove(CoreAnnotations.TextAnnotation.class); // Ensure text is removed
      List<CoreLabel> compactTokens = new ArrayList<>();

      for (var token : AnnotationUtils.wordTokens(sentence)) {
        CoreLabel compactToken = new CoreLabel();
        compactToken.set(CoreAnnotations.LemmaAnnotation.class, token.lemma());
        compactToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, token.tag());
        compactToken.set(CoreAnnotations.TextAnnotation.class, token.word());
        compactTokens.add(compactToken);
      }

      sentence.set(CoreAnnotations.TokensAnnotation.class, compactTokens);
      dest.get(CoreAnnotations.SentencesAnnotation.class).add(sentence);
    }
    return dest;
  }

}
