package dev.taras.hamkalo.plagdet.preprocessing;

import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations.CodeSegmentsAnnotation;
import static dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations.TemplateSentencesAnnotation;

public class AnnotationSerializer {

  public static @NotNull String serialize(@NotNull Annotation annotation) {
    if (annotation.size() == 0) {
      return "";
    }
    var sentences = AnnotationUtils.sentences(annotation);
    int sentencesSize = sentences.size();
    StringJoiner stringJoiner = new StringJoiner("\n");
    stringJoiner.add(serializeArraysList(annotation, TemplateSentencesAnnotation.class));
    stringJoiner.add(serializeArraysList(annotation, CodeSegmentsAnnotation.class));
    for (int i = 0; i < sentencesSize; i++) {
      stringJoiner.add(serializeSentence(sentences.get(i)));
    }

    return stringJoiner.toString();
  }

  public static @NotNull Annotation deserialize(@NotNull String content) {
    Annotation annotation = new Annotation(content);
    List<CoreMap> sentences = new ArrayList<>();

    var lines = content.split("\n");
    int linesSize = lines.length;
    if (linesSize > 0) {
      deserializeArraysListToAnnotation(
        annotation, TemplateSentencesAnnotation.class, lines[0], 2
      );
    }

    if (linesSize > 1) {
      deserializeArraysListToAnnotation(
        annotation, CodeSegmentsAnnotation.class, lines[1], 2
      );
    }

    for (int i = 2; i < linesSize; i++) {
      sentences.add(deserializeSentence(lines[i]));
    }

    annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);
    annotation.set(
      CoreAnnotations.TokensAnnotation.class,
      sentences.stream().flatMap(s -> AnnotationUtils.tokens(s).stream()).toList()
    );

    return annotation;
  }

  /**
   * Serializes int[] list into single line, each element separated by space character
   */
  public static @NotNull String serializeArraysList(
    @NotNull Annotation source,
    @NotNull Class<? extends CoreAnnotation<List<int[]>>> annotationClazz
  ) {
    List<int[]> arrays = source.get(annotationClazz);
    if (arrays == null || arrays.isEmpty()) {
      return "";
    }

    return arrays.stream()
             .map(arr ->
               Arrays.stream(arr)
                 .mapToObj(Integer::toString)
                 .collect(Collectors.joining(",")))
             .collect(Collectors.joining(";"));
  }

  /**
   * Deserializes arrays list from line and places it into annotation with key annotationClazz
   * In case of blank line, empty list is set
   */
  public static void deserializeArraysListToAnnotation(
    @NotNull Annotation dest,
    @NotNull Class<? extends CoreAnnotation<List<int[]>>> annotationClazz,
    @NotNull String line,
    int maxArraySize
  ) {
    if (line.isBlank()) {
      dest.set(annotationClazz, Collections.emptyList());
      return;
    }

    List<int[]> deserialized = new ArrayList<>();
    String[] arrayStrings = line.split(";");
    for (String arrayString : arrayStrings) {
      String[] intStrings = arrayString.split(",");
      int[] ints = new int[maxArraySize];
      for (int i = 0; i < intStrings.length; i++) {
        if (i < maxArraySize) {
          ints[i] = Integer.parseInt(intStrings[i]);
        }
      }
      deserialized.add(ints);
    }

    dest.set(annotationClazz, deserialized);
  }

  public static @NotNull CoreMap deserializeSentence(@NotNull String line) {
    String[] parts = line.split("\t");
    String idString = parts[0];
    String[] range = parts[1].split("-");
    int begin = Integer.parseInt(range[0]);
    int end = Integer.parseInt(range[1]);

    String[] tokenPosLemmas = parts.length > 2 ? parts[2].split(" ") : new String[0];
    int tokensSize = tokenPosLemmas.length;
    var tokens = new ArrayList<CoreLabel>();
    for (int i = 0; i < tokensSize; i++) {
      CoreLabel token = new CoreLabel();
      var tokenParts = tokenPosLemmas[i].split(":");
      token.setLemma(tokenParts[1]);
      token.setTag(tokenParts[0]);
      tokens.add(token);
    }

    var deserialized = AnnotationUtils.createSentence(tokens, begin, end);
    deserialized.set(PlagdetAnnotations.TextBlockId.class, Integer.parseInt(idString));
    return deserialized;
  }

  public static @NotNull String serializeSentence(@NotNull CoreMap sentence) {
    StringJoiner tokenJoiner = new StringJoiner(" ");
    var tokens = AnnotationUtils.tokens(sentence);
    var id = AnnotationUtils.textBlockId(sentence);
    int tokensSize = tokens.size();
    for (int i = 0; i < tokensSize; i++) {
      var token = tokens.get(i);
      tokenJoiner.add(String.format("%s:%s", token.tag(), token.lemma()));
    }

    return String.format(
      "%d\t%d-%d\t%s", id, AnnotationUtils.begin(sentence), AnnotationUtils.end(sentence), tokenJoiner
    );
  }
}
