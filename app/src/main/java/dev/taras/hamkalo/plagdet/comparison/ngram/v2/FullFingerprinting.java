package dev.taras.hamkalo.plagdet.comparison.ngram.v2;

import demos.WordAnnotatorDemo;
import dev.taras.hamkalo.plagdet.comparison.ngram.v2.shingles.Shingles;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.presentation.TextAnnotator;

public class FullFingerprinting {

  public void compare(DocumentEntry first, DocumentEntry second) {
    final int shingleSize = 8;

    var firstShingles = Shingles.createShingles(first, shingleSize);
    var secondShingles = Shingles.createShingles(second, shingleSize);

    var sharedShingles = Shingles.pairShingles(firstShingles, secondShingles);

//  *** merging
    var sharedFragments = PostProcessing.expandAndFilter(sharedShingles);

    TextAnnotator textAnnotator = new TextAnnotator();
    textAnnotator.createAnnotatedDocuments(first, second, sharedFragments);

//    System.out.println("Jaccard on WNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToString));
//    System.out.println("Jaccard on CNG: " + jaccardSimilarity(documentNgrams1, documentNgrams2, this::ngramToSortedString));
  }
}
