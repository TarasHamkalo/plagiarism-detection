package dev.taras.hamkalo.plagdet.comparison.ngram;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.preprocessing.Token;
import edu.stanford.nlp.util.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FullFingerprintingComparator {

  public void compare(DocumentEntry first, DocumentEntry second) {
    final int ngramSize = 8;

    var documentNgrams1 = createWng(first, ngramSize);
    var documentNgrams2 = createWng(second, ngramSize);

    System.out.println("Jaccard on WNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToString));
    System.out.println("Jaccard on CNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToSortedString));

    var skigrams1 = createSwng(first, 3, 5);
    var skigrams2 = createSwng(second, 3, 5);
    System.out.println("Jaccard on SWNG: " + jaccardSimilarity(skigrams1, skigrams2, this::ngramToString));
    System.out.println("Jaccard on CSNG: " + jaccardSimilarity(skigrams1, skigrams2, this::ngramToSortedString));
  }

  // Overlap n - 1
  public List<Ngram> createWng(DocumentEntry document, int n) {
    final var tokens = document.getTokens();
    var ngrams = new ArrayList<Ngram>();
    for (int i = 0; i < tokens.size() - n + 1; i++) {
      var ngram = new ArrayList<String>();
      for (int j = i; j < i + n; j++) {
        ngram.add(tokens.get(j).getContent());
      }


      ngrams.add(Ngram.builder()
        .contentStrings(ngram)
        .start(tokens.get(i).getStart())
        .end(tokens.get(i + n - 1).getEnd())
        .build()
      );
    }

    return ngrams;
  }

  // a b c d e f g
  // skip = 2, n = 3
  // a - - d e
  public List<Ngram> createSwng(DocumentEntry document, int n, int k) {
    final var tokens = document.getTokens();

//    var skipgrams = new ArrayList<Ngram>();
    var result = new ArrayList<Ngram>();
    for (int i = 0; i < tokens.size() - n - k + 1; i++) {
      createSubsequences(tokens, i, n, k, new ArrayList<>(), result);
    }

    return result;
  }

  // subsequences in which k skips are in front should be filtered
  private static void createSubsequences(
    List<Token> tokens, int i, int n, int k, List<Token> current, List<Ngram> result) {

    if (current.size() == n) {
      var skipgram = Ngram.builder()
        .start(current.get(0).getStart())
        .end(current.get(current.size() - 1).getEnd())
        .contentStrings(current.parallelStream().map(Token::getContent).collect(Collectors.toList()))
        .build();
      result.add(skipgram);
      return;
    }

    // Generate combinations within the skip limit
    for (int j = i; j < tokens.size() && (j - i) <= k; j++) {
      current.add(tokens.get(j));  // Include this token
      createSubsequences(tokens, j + 1, n, k, current, result);
      current.remove(current.size() - 1);  // Backtrack
    }
  }

  public double jaccardSimilarity(List<Ngram> ngrams1,
                                  List<Ngram> ngrams2,
                                  Function<Ngram, String> mapper) {
    var ngramsSet1 = ngrams1.parallelStream().map(mapper).collect(Collectors.toSet());
    var ngramsSet2 = ngrams2.parallelStream().map(mapper).collect(Collectors.toSet());
    double unionSize = Sets.union(ngramsSet1, ngramsSet2).size() * 1.0;
    double intersectionSize = Sets.intersection(ngramsSet1, ngramsSet2).size() * 1.0;
    System.out.println(intersectionSize);
    System.out.println(unionSize);
    return intersectionSize / unionSize;
  }


  public String ngramToString(Ngram ngram) {
    return ngram.getContentStrings().stream()
      .reduce("", (agg, s) -> agg + s);
  }

  public String ngramToSortedString(Ngram ngram) {
    return ngram.getContentStrings().stream()
      .sorted()
      .reduce("", (agg, s) -> agg + s);
  }

}
