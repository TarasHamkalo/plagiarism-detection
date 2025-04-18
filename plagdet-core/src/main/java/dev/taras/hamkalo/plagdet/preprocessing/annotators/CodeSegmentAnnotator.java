package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.classification.code.FeatureExtractor;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static dev.taras.hamkalo.plagdet.classification.code.FeatureExtractor.FEATURE_VECTOR_SIZE;

@Slf4j
public class CodeSegmentAnnotator implements Annotator {

  private static final int MIN_CODE_LINES_TO_RECORD = 2;

  public static final String CLASSIFIER_RESOURCE_PREFIX = "models/";

  public static final String CLASSIFIER_RESOURCE_NAME = "code-classifier.bin";

  private static MultiLayerNetwork codeTextClassifier;

  public CodeSegmentAnnotator() {
    loadClassifier();
  }

  private static synchronized void loadClassifier() {
    if (codeTextClassifier == null) {
      try {
        var classifierPath = IoUtils.resourceFromTemp(
          CLASSIFIER_RESOURCE_PREFIX + CLASSIFIER_RESOURCE_NAME, CLASSIFIER_RESOURCE_NAME
        );
        codeTextClassifier = ModelSerializer.restoreMultiLayerNetwork(classifierPath.toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Code segment annotator started");
    sentenceRemoval(annotation);
  }

  private static void sentenceRemoval(Annotation annotation) {
    List<CoreMap> sentences = AnnotationUtils.sentences(annotation);
    List<CoreMap> filteredSentences = new ArrayList<>(sentences.size() / 2);

    int sentencesSize = sentences.size();
    int[][] lineOffsets = new int[sentencesSize][2];
    List<int[]> codeSegments = new ArrayList<>();
    List<Integer> textBlockBoundaries = new ArrayList<>();

    INDArray vectorizedLines = vectorizeSentences(sentences, lineOffsets);
    int[] linesClass = codeTextClassifier.predict(vectorizedLines);

    List<Integer> codeLines = new ArrayList<>();
    int codeLinesOffsetBegin = 0;
    int codeLinesBeginIndex = 0;
    int codeLinesCount = 0;
    for (int i = 0; i < sentencesSize; i++) {
      CoreMap sentence = sentences.get(i);
      if (linesClass[i] == 1) {
        String line = sentence.get(CoreAnnotations.TextAnnotation.class);
        codeLines.add(i);
        codeLinesCount += line.isBlank() ? 0 : 1;
        continue;
      }

      if (codeLinesCount >= MIN_CODE_LINES_TO_RECORD) {
        int codeLinesOffsetEnd = lineOffsets[i][0] - 1; // before current line
        codeSegments.add(new int[]{codeLinesOffsetBegin, codeLinesOffsetEnd});
        textBlockBoundaries.add(codeLinesBeginIndex);
      } else {
        for (int sentenceIdx : codeLines) {
          filteredSentences.add(sentences.get(sentenceIdx));
        }
      }

      filteredSentences.add(sentence);
      codeLinesOffsetBegin = lineOffsets[i][1] + 1; // after current line
      codeLinesBeginIndex = filteredSentences.size() - 1; // possibly the next sentence
      codeLinesCount = 0;
      codeLines.clear();
    }

    if (codeLinesCount >= MIN_CODE_LINES_TO_RECORD) {
      var text = annotation.get(CoreAnnotations.TextAnnotation.class);
      codeSegments.add(new int[]{codeLinesOffsetBegin, text.length()});
      textBlockBoundaries.add(codeLinesBeginIndex);
    } else {
      for (int sentenceIdx : codeLines) {
        filteredSentences.add(sentences.get(sentenceIdx));
      }
    }

    annotation.set(PlagdetAnnotations.TextBlockBoundaries.class, textBlockBoundaries);
    annotation.set(PlagdetAnnotations.CodeSegmentsAnnotation.class, codeSegments);
    annotation.set(CoreAnnotations.SentencesAnnotation.class, filteredSentences);
  }

  private static INDArray vectorizeSentences(List<CoreMap> sentences, int[][] lineOffsets) {
    int sentencesSize = sentences.size();
    int beginOffset = 0;
    INDArray encodedLines = Nd4j.create(sentencesSize, FEATURE_VECTOR_SIZE);
    for (int i = 0; i < sentencesSize; i++) {
      String line = sentences.get(i).get(CoreAnnotations.TextAnnotation.class);
      double[] features = FeatureExtractor.extractFeatures(line);
      encodedLines.putRow(i, Nd4j.create(features));
      lineOffsets[i][0] = beginOffset;
      lineOffsets[i][1] = beginOffset + line.length();
      beginOffset += line.length() + 1;
    }
    return encodedLines;
  }


  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of(PlagdetAnnotations.CodeSegmentsAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(PlagdetAnnotations.GuessedLemmaAnnotation.class, CoreAnnotations.SentencesAnnotation.class);
  }
}
