package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import dev.taras.hamkalo.plagdet.preprocessing.annotators.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Properties;

public abstract class Pipeline {

  @Value
  @Builder
  public static class PipelineParameters {

    public static final boolean DEFAULT_BYPASS_TEMPLATE_REMOVAL = false;

    public static final boolean DEFAULT_BYPASS_SPELLCHECK = false;

    @Builder.Default
    boolean canBypassTemplateRemoval = DEFAULT_BYPASS_TEMPLATE_REMOVAL;

    @Builder.Default
    boolean canBypassSpellCheck = DEFAULT_BYPASS_TEMPLATE_REMOVAL;
  }

  private static StanfordCoreNLP firstStagePipeline;

  private static StanfordCoreNLP templateComparisonPipeline;

  private static StanfordCoreNLP luceneDocumentPipeline;

  public static StanfordCoreNLP getFirstStagePipeline(boolean canBypassSpellcheck) {
    if (firstStagePipeline == null) {

      var props = new Properties();
      props.put("annotators", "tokenize, ssplit");
      props.put("ssplit.eolonly", "true");
      props.put("ssplit.boundaryTokenRegex", "(:?[a-zA-Z]\\.)|[.。]|[!?！？]+");
      props.put("tokenize.options", "invertible=true");
      props.put("enforceRequirements", "false");

      firstStagePipeline = new StanfordCoreNLP(props);
      firstStagePipeline.addAnnotator(new CodeSegmentAnnotator());
      firstStagePipeline.addAnnotator(new TextBlockIdentifierAnnotator());
      firstStagePipeline.addAnnotator(new TextBlockSentenceSplitter());
      firstStagePipeline.addAnnotator(new MorphoDiTaAnalysisAnnotator());
      firstStagePipeline.addAnnotator(new WordFilteringAnnotator());
      firstStagePipeline.addAnnotator(new StopWordAnnotator());
      if (!canBypassSpellcheck) {
        firstStagePipeline.addAnnotator(new SpellingAnnotator());
        firstStagePipeline.addAnnotator(new MorphoDiTaAnalysisAnnotator());
      }
    }

    return firstStagePipeline;
  }

  public static StanfordCoreNLP getTemplateComparisonPipeline(
    @NotNull Embeddings embeddings,
    @Nullable Annotation templateAnnotation
  ) {
    if (templateComparisonPipeline == null) {
      var props = new Properties();
      props.put("annotators", "");
      props.put("enforceRequirements", "false");
      templateComparisonPipeline = new StanfordCoreNLP(props);
      templateComparisonPipeline.addAnnotator(new TemplateSentencesAnnotator(embeddings, templateAnnotation));
      templateComparisonPipeline.addAnnotator(new TextBlockIdentifierAnnotator());
    }

    return templateComparisonPipeline;
  }

  private static StanfordCoreNLP getLuceneDocumentPipeline() {
    if (luceneDocumentPipeline == null) {
      var props = new Properties();
      props.put("annotators", "lucene");
      props.put(
        "customAnnotatorClass.lucene",
        "dev.taras.hamkalo.plagdet.preprocessing.annotators.LuceneStoredDocumentAnnotator"
      );

      props.put("enforceRequirements", "false");
      luceneDocumentPipeline = new StanfordCoreNLP(props);
    }

    return luceneDocumentPipeline;
  }

  public static void init(
    @NotNull PipelineParameters parameters,
    @NotNull Embeddings embeddings,
    @Nullable String templateContent
  ) {
    firstStagePipeline = getFirstStagePipeline(parameters.canBypassSpellCheck);
    luceneDocumentPipeline = getLuceneDocumentPipeline();
    if (!parameters.canBypassTemplateRemoval) {
      if (templateContent != null) {
        templateComparisonPipeline = getTemplateComparisonPipeline(
          embeddings, annotate(templateContent)
        );
      } else {
        templateComparisonPipeline = getTemplateComparisonPipeline(embeddings, null);
      }
    }
  }

  public static @NotNull Annotation luceneAnnotate(@NotNull String content) {
    CoreDocument coreDocument = new CoreDocument(content);
    luceneDocumentPipeline.annotate(coreDocument);
    var annotation = coreDocument.annotation();
    annotation.remove(CoreAnnotations.TextAnnotation.class);
    return annotation;
  }

  public static @NotNull Annotation annotate(@NotNull String content) {
    CoreDocument coreDocument = new CoreDocument(content);
    firstStagePipeline.annotate(coreDocument);
    if (templateComparisonPipeline != null) {
      templateComparisonPipeline.annotate(coreDocument);
    }

    // spell -> misspelled, tag, pos, code sentences, spoofed
    Annotation dest = new Annotation("");
    Annotation source = coreDocument.annotation();

    AnnotationUtils.copy(PlagdetAnnotations.CodeSegmentsAnnotation.class, dest, source);
    AnnotationUtils.copy(PlagdetAnnotations.TemplateSentencesAnnotation.class, dest, source);
    createCompactSentencesAndTokens(dest, source);
    return dest;
  }

  private static void createCompactSentencesAndTokens(
    @NotNull Annotation dest,
    @NotNull Annotation source
  ) {
    dest.set(CoreAnnotations.SentencesAnnotation.class, new ArrayList<>());
    dest.set(CoreAnnotations.TokensAnnotation.class, new ArrayList<>());
    for (var sentence : AnnotationUtils.sentences(source)) {
      sentence.remove(CoreAnnotations.TextAnnotation.class);
      var tokens = AnnotationUtils.tokens(sentence);
      var compactTokens = new ArrayList<CoreLabel>();
      for (var token : tokens) {
        compactTokens.add(createCompactToken(token));
      }

      sentence.set(CoreAnnotations.TokensAnnotation.class, compactTokens);
      dest.get(CoreAnnotations.TokensAnnotation.class).addAll(compactTokens);
      dest.get(CoreAnnotations.SentencesAnnotation.class).add(sentence);
    }
  }

  public static @NotNull CoreLabel createCompactToken(@NotNull CoreLabel token) {
    CoreLabel compactToken = new CoreLabel();
    compactToken.set(CoreAnnotations.LemmaAnnotation.class, token.lemma());
    compactToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, token.tag());
    compactToken.setBeginPosition(token.beginPosition());
    compactToken.setEndPosition(token.endPosition());

    if (AnnotationUtils.isMisspelledToken().test(token)) {
      AnnotationUtils.copy(PlagdetAnnotations.MisspelledAnnotation.class, compactToken, token);
      compactToken.set(CoreAnnotations.TextAnnotation.class, token.word());
    }

    if (AnnotationUtils.isSpoofedToken().test(token)) {
      AnnotationUtils.copy(PlagdetAnnotations.SpoofedWordAnnotation.class, compactToken, token);
      compactToken.set(CoreAnnotations.TextAnnotation.class, token.word());
    }
    return compactToken;
  }

}
