package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Properties;

public abstract class Pipeline {

  private static StanfordCoreNLP firstStagePipeline;

  private static StanfordCoreNLP secondStagePipeline;

  public static StanfordCoreNLP getFirstStagePipeline() {
    if (firstStagePipeline == null) {

      var props = new Properties();
      props.put("annotators", "tokenize, ssplit, morpho, code, dynamicssplit, word, stopword, spell");//, code, spell");
      props.put("ssplit.eolonly", "false");
      props.put("ssplit.newlineIsSentenceBreak", "two");

      props.put(
        "customAnnotatorClass.code",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.CodeSegmentAnnotator"
      );

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
    Annotation source = coreDocument.annotation();

    AnnotationUtils.copy(PlagdetAnnotations.CodeSentencesAnnotation.class, dest, source);
    dest.set(CoreAnnotations.SentencesAnnotation.class, new ArrayList<>());
    dest.set(CoreAnnotations.TokensAnnotation.class, new ArrayList<>());
    for (var sentence : AnnotationUtils.sentences(source)) {
      sentence.remove(CoreAnnotations.TextAnnotation.class);
      var tokens = AnnotationUtils.wordTokens(sentence);
      var compactTokens = new ArrayList<CoreLabel>();
      for (var token : tokens) {
        CoreLabel compactToken = new CoreLabel();
        compactToken.set(CoreAnnotations.LemmaAnnotation.class, token.lemma());
        compactToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, token.tag());
        compactToken.set(CoreAnnotations.TextAnnotation.class, token.word());
        compactToken.setBeginPosition(token.beginPosition());
        compactToken.setEndPosition(token.endPosition());
        AnnotationUtils.copy(PlagdetAnnotations.MisspelledAnnotation.class, compactToken, token);
        AnnotationUtils.copy(PlagdetAnnotations.SpoofedWordAnnotation.class, compactToken, token);
        compactTokens.add(compactToken);
      }

      sentence.set(CoreAnnotations.TokensAnnotation.class, compactTokens);

      dest.get(CoreAnnotations.TokensAnnotation.class).addAll(compactTokens);
      dest.get(CoreAnnotations.SentencesAnnotation.class).add(sentence);
    }
    System.gc();
    return dest;
  }

}
