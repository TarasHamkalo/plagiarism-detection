package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.classification.code.FeatureExtractor;
import dev.taras.hamkalo.plagdet.io.IoUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
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

    var text = annotation.get(CoreAnnotations.TextAnnotation.class);
    var lines = text.split("\n");
    int linesSize = lines.length;

    int[][] lineOffsets = new int[linesSize][2];
    List<int[]> codeSentences = new ArrayList<>();
    StringJoiner stringJoiner = new StringJoiner("\n");

    INDArray vectorizedLines = vectorizeLines(lines, lineOffsets);
    int[] linesClass = codeTextClassifier.predict(vectorizedLines);

    List<String> codeLines = new ArrayList<>();
    int codeLinesOffsetBegin = 0;
    int codeLinesCount = 0;
    for (int i = 0; i < linesSize; i++) {
      String line = lines[i];
      if (linesClass[i] == 1) {
        codeLines.add(line);
        codeLinesCount += line.isBlank() ? 0 : 1;
        continue;
      }

      if (codeLinesCount >= MIN_CODE_LINES_TO_RECORD) {
        int codeLinesOffsetEnd = lineOffsets[i][0] - 1; // before current line
        codeSentences.add(new int[]{codeLinesOffsetBegin, codeLinesOffsetEnd});
        stringJoiner.add(" ".repeat(codeLinesOffsetEnd - codeLinesOffsetBegin));
      } else {
        for (String codeLine : codeLines) {
          stringJoiner.add(codeLine);
        }
      }
      codeLines.clear();
      codeLinesCount = 0;

      stringJoiner.add(line);
      codeLinesOffsetBegin = lineOffsets[i][1] + 1; // after current line
    }

    if (codeLinesCount >= MIN_CODE_LINES_TO_RECORD) {
      stringJoiner.add(" ".repeat(text.length() - codeLinesOffsetBegin));
      codeSentences.add(new int[]{codeLinesOffsetBegin, text.length()});
    } else {
      for (String codeLine : codeLines) {
        stringJoiner.add(codeLine);
      }
    }

    annotation.set(CoreAnnotations.TextAnnotation.class, stringJoiner.toString());
    annotation.set(PlagdetAnnotations.CodeSentencesAnnotation.class, codeSentences);
  }

  private static INDArray vectorizeLines(String[] lines, int[][] lineOffsets) {
    int linesSize = lines.length;
    int beginOffset = 0;
    INDArray encodedLines = Nd4j.create(linesSize, FEATURE_VECTOR_SIZE);
    for (int i = 0; i < linesSize; i++) {
      String line = lines[i];
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
    return Set.of(PlagdetAnnotations.CodeSentencesAnnotation.class);
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(PlagdetAnnotations.GuessedLemmaAnnotation.class, CoreAnnotations.SentencesAnnotation.class);
  }
}
