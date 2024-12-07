package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.preprocessing.Token;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Shingles {

  /**
   * @param shingles document shingles
   * @param other    document shingles
   * @return all pairs of similar shingles in form (s1 of shignles, s2 of other)
   */
  public static List<SharedShingle> pairShingles(List<Shingle> shingles, List<Shingle> other) {
    List<SharedShingle> result = new ArrayList<>();

    var sortedShingles = createSortedCopy(shingles);
    var otherSorted = createSortedCopy(other);

    for (var first : sortedShingles) {
      for (var second : otherSorted) {
        // TODO: fuzzy similarity here
        if (first.getContent().equals(second.getContent())) {
          // TODO: не ідентифікується з якого документу перший чи другий
          result.add(SharedShingle.from(first, second));
        }
      }
    }

    return result;
  }

  private static ArrayList<Shingle> createSortedCopy(List<Shingle> shingles) {
    var sortedShingles = new ArrayList<>(shingles);
    sortedShingles.sort(Comparator.comparing(Shingle::getContent));
    return sortedShingles;
  }

  public static List<Shingle> createShingles(DocumentEntry document, int n) {
    final var tokens = document.getTokens();
    var shingles = new ArrayList<Shingle>();
    var shingle = new ArrayList<Token>();
    for (int i = 0; i < tokens.size() - n + 1; i++) {
      if (i < n) {
        shingle.add(tokens.get(i));
        continue;
      }

      if (i > n) {
        shingle.remove(0);
        shingle.add(tokens.get(i));
      }

      shingles.add(Shingle.builder()
        .start(shingle.get(0).getStart())
        .end(shingle.get(shingle.size() - 1).getEnd())
        .content(Shingles.toSortedString(shingle))
        .build()
      );

    }

    return shingles;
  }

  public static String toString(List<Token> tokens) {
    return tokens.stream().map(Token::getContent).collect(Collectors.joining(" "));
  }

  public static String toSortedString(List<Token> tokens) {
    return tokens.stream().map(Token::getContent).sorted().collect(Collectors.joining(" "));
  }

}
