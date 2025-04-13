package dev.taras.hamkalo.plagdet.indexing.lucene.mapping;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneConstants;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.StringJoiner;

import static org.apache.lucene.document.Field.Store;

public class StanfordAnnotationMapper {

  public static void addPreprocessedContent(
    @NotNull DocumentEntry documentEntry,
    @NotNull Document doc
  ) {
    var sentences = AnnotationUtils.sentences(documentEntry.getAnnotation());
    int sentencesSize = sentences.size();
    StringJoiner stringJoiner = new StringJoiner("\n");
    for (int i = 0; i < sentencesSize; i++) {
      stringJoiner.add(serialize(sentences.get(i)));
    }

    doc.add(
      new TextField(LuceneConstants.PREPROCESSED_CONTENT, stringJoiner.toString(), Store.YES)
    );
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
