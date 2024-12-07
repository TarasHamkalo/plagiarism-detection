package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingles;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.presentation.TextAnnotator;

public class FullFingerprinting {

  public void compare(DocumentEntry first, DocumentEntry second) {
    final int shingleSize = 8;

    var firstShingles = Shingles.createShingles(first, shingleSize);
    var secondShingles = Shingles.createShingles(second, shingleSize);

    var sharedShingles = Shingles.pairShingles(firstShingles, secondShingles);

//  ** merging
    var sharedFragments = PostProcessing.expandAndFilter(sharedShingles);

    TextAnnotator textAnnotator = new TextAnnotator();
    textAnnotator.createAnnotatedDocuments(first, second, sharedFragments);

    int firstCovered = 0;
    int secondCovered = 0;
    for (var fragment : sharedFragments) {
      firstCovered += fragment.firstCovered();
      secondCovered += fragment.secondCovered();
    }

    int totalSize = first.getContent().length() + second.getContent().length();
    double avgSimilarity = (firstCovered + secondCovered * 1.0) / totalSize;
    System.out.println("Avg similarity: " + avgSimilarity);

    double firstCoverage = firstCovered / (first.getContent().length() * 1.0);
    double secondCoverage = secondCovered / (second.getContent().length() * 1.0);
    double maxSimilarity = Math.max(firstCoverage, secondCoverage);
    System.out.println("Max similarity: " + maxSimilarity);

//    System.out.println("Jaccard on WNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToString));
//    System.out.println("Jaccard on CNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToSortedString));
  }
}
