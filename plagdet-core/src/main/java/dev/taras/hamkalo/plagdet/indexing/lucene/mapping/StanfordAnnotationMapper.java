package dev.taras.hamkalo.plagdet.indexing.lucene.mapping;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations.CodeSentencesAnnotation;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations.TemplateSentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.apache.lucene.document.Field.Store;

public class StanfordAnnotationMapper {

  public static void addPreprocessedContent(
    @NotNull DocumentEntry documentEntry,
    @NotNull Document doc
  ) {
    var annotation = documentEntry.getAnnotation();
    var sentences = AnnotationUtils.sentences(annotation);
    int sentencesSize = sentences.size();
    StringJoiner stringJoiner = new StringJoiner("\n");
    stringJoiner.add(serializeArraysList(annotation, TemplateSentencesAnnotation.class));
    stringJoiner.add(serializeArraysList(annotation, CodeSentencesAnnotation.class));
    for (int i = 0; i < sentencesSize; i++) {
      stringJoiner.add(serialize(sentences.get(i)));
    }

    doc.add(
      new TextField(LuceneConstants.PREPROCESSED_CONTENT, stringJoiner.toString(), Store.YES)
    );
  }

  /**
   * Serializes int[] list into single line, each element separated by space character
   */
  public static String serializeArraysList(
    @NotNull Annotation source,
    @NotNull Class<? extends CoreAnnotation<List<int[]>>> annotationClazz
  ) {
    List<int[]> arrays = source.get(annotationClazz);
    if (arrays == null) {
      return "\n";
    }

    StringJoiner stringJoiner = new StringJoiner(" ");
    arrays.stream().map(Arrays::toString).forEach(stringJoiner::add);
    stringJoiner.add("\n");
    return stringJoiner.toString();
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
    }

    List<int[]> deserialized = new ArrayList<>();
    String[] arrayStrings = line.split(" ");
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

  public static @NotNull CoreMap deserialize(String line) {
    String[] parts = line.split("\t");
    String[] range = parts[0].split("-");
    int begin = Integer.parseInt(range[0]);
    int end = Integer.parseInt(range[1]);

    String[] tokenPosLemmas = parts.length > 1 ? parts[1].split(" ") : new String[0];
    int tokensSize = tokenPosLemmas.length;
    var tokens = new ArrayList<CoreLabel>();
    for (int i = 0; i < tokensSize; i++) {
      CoreLabel token = new CoreLabel();
      var tokenParts = tokenPosLemmas[i].split(":");
      token.setLemma(tokenParts[1]);
      token.setTag(tokenParts[0]);
      tokens.add(token);
    }

    return AnnotationUtils.createSentence(tokens, begin, end);
  }

  public static @NotNull String serialize(@NotNull CoreMap sentence) {
    StringJoiner tokenJoiner = new StringJoiner(" ");
    var tokens = AnnotationUtils.tokens(sentence);
    int tokensSize = tokens.size();
    for (int i = 0; i < tokensSize; i++) {
      var token = tokens.get(i);
      tokenJoiner.add(String.format("%s:%s", token.tag(), token.lemma()));
    }

    return String.format(
      "%d-%d\t%s", AnnotationUtils.begin(sentence), AnnotationUtils.end(sentence), tokenJoiner
    );
  }
}
