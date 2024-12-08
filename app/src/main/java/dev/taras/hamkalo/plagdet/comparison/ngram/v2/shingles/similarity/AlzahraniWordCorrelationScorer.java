package dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.similarity;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingle;

/**
 * Computes mean number of words from first shingle also present in the second shingle
 * and vice versa, taking maximum value from it
 * https://downloads.webis.de/pan/publications/papers/alzahrani_2015.pdf
 */
public class AlzahraniWordCorrelationScorer implements SimilarityScorer {


  @Override
  public double score(Shingle first, Shingle second) {
    return Math.max(correlationScore(first, second), correlationScore(second, first));
  }

  private double correlationScore(Shingle first, Shingle second) {
    double score = 0;
    var words = first.getContent().split(" ");
    for (var shingleWord : words) {
       score += wordCorrelationScore(shingleWord, second);
    }
    return score / words.length;
  }

  private double wordCorrelationScore(String word, Shingle shingle) {
    double correlation = 1;
    for (var shingleWord : shingle.getContent().split(" ")) {
      correlation *= 1 - (shingleWord.equals(word) ? 1 : 0);
    }

    return 1 - correlation;
  }


//  public static void main(String[] args) {
//    var s1 = "rúra je jednosmerný komunikačný prostriedok";
//    var s2 = "rúra predstavuje jednosmerný komunikačný kanál";
//
//    s1 = "quick brown fox jumps over lazy dog";
//    s2 = "fast brown fox leaps over sleepy dog";
//
//    var first = new Shingle(s1, 0, 43);
//    var second = new Shingle(s2, 0, 46);
//
//    var wordCorrelationScorer = new AlzahraniWordCorrelationScorer();
//    var jaccardScorer = new JaccardScorer();
//    var diceScorer = new DiceScorer();
//
//    System.out.println(wordCorrelationScorer.score(first, second));
//    System.out.println(jaccardScorer.score(first, second));
//    System.out.println(diceScorer.score(first, second));
//
////    , kde dáta zapisované na jednom konci sú čítané na druhom konci (jednosmerný tok dát). Vytvára sa volaním funkcie pipe(), pričom systém alokuje dva deskriptory súborov pre komunikáciu. Tieto deskriptory sú dedené každým potomkom procesu.
//  }
}
