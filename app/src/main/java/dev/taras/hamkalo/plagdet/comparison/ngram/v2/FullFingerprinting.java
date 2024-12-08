package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingles;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.presentation.TextAnnotator;
import dev.taras.hamkalo.plagdet.presentation.XmlAnnotator;

public class FullFingerprinting {

  public void compare(DocumentEntry first, DocumentEntry second) {
    final int shingleSize = 8;
    final int overlap = 3;

    var firstShingles = Shingles.createShingles(first, shingleSize, overlap);
    var secondShingles = Shingles.createShingles(second, shingleSize, overlap);

    var sharedShingles = Shingles.pairShingles(firstShingles, secondShingles);

//  ** merging
    var sharedFragments = PostProcessing.expandAndFilter(sharedShingles);

    TextAnnotator textAnnotator = new TextAnnotator();
    textAnnotator.createAnnotatedDocuments(first, second, sharedFragments);
    XmlAnnotator xmlAnnotator = new XmlAnnotator();

    xmlAnnotator.createXMLAnnotatedMatches(first, second, sharedFragments);

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
    System.out.println("First covered: " +  firstCoverage);
    System.out.println("Second covered: " +  secondCoverage);

//    System.out.println("Jaccard on WNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToString));
//    System.out.println("Jaccard on CNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToSortedString));
  }
}
