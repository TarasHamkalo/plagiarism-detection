/**
 * This class implements filtering strategy of the plagiarism detection algorithm
 * described in the following papers:
 * <p>
 * 1. Sanchez-Perez, M.A., Gelbukh, A., Sidorov, G.:
 *    Adaptive algorithm for plagiarism detection: The best-performing approach at PAN 2014 text alignment competition.
 *    Lecture Notes in Computer Science, vol. 9283, Springer, 2015, pp. 402-413.
 * <p>
 * 2. Sanchez-Perez, M.A., Gelbukh, A.F., Sidorov, G.:
 *    Dynamically adjustable approach through obfuscation type recognition.
 *    Working Notes of CLEF 2015 - Conference and Labs of the Evaluation forum, Toulouse, France, September 8-11, 2015.
 *    CEUR Workshop Proceedings, vol. 1391, CEUR-WS.org, 2015, http://ceur-ws.org/Vol-1391/92-CR.pdf.
 * <p>
 * Original source: https://www.gelbukh.com/plagiarism-detection/PAN-2015/index.html
 * License: Free for non-commercial academic purposes.
 */

package dev.taras.hamkalo.plagdet.comparision.comparator.blocks;

import dev.taras.hamkalo.plagdet.comparision.Fragment;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FilteringStrategy {

  Embeddings embeddings;

  int minFragmentCharLength;

  public List<Fragment> filter(List<Fragment> fragments) {
//    var filteredFragments = removeOverlapping(fragments);
    var filteredFragments = removeShort(fragments);
    return filteredFragments;
  }

  private List<Fragment> removeShort(List<Fragment> fragments) {
    var filteredFragments = new ArrayList<Fragment>();
    for (var fragment : fragments) {
      fragment.sortSeedsBySideIndex(0);
      var firstSideLength =
        fragment.lastSeed().getFirstSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetEndAnnotation.class) -
        fragment.firstSeed().getFirstSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);

      fragment.sortSeedsBySideIndex(1);

      var secondSideLength =
        fragment.lastSeed().getSecondSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetEndAnnotation.class) -
        fragment.firstSeed().getSecondSent().coreMap()
          .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);

      if (firstSideLength > minFragmentCharLength &&
          secondSideLength > minFragmentCharLength) {
        filteredFragments.add(fragment);
      }
    }

    return filteredFragments;
  }

  private List<Fragment> removeOverlapping(List<Fragment> fragments) {
    // after that sorting, whenever we find non overlapping cluster,
    // there is no need to proceed further
    for (var fragment : fragments) {
      fragment.sortSeedsBySideIndex(0);
    }

    fragments.sort(Comparator.comparingInt(c -> c.getStartSideIndex(0)));
    var filteredFragments = new ArrayList<Fragment>();

    int i = 0;
    while (i < fragments.size()) {
      var source = fragments.get(i);
      int j = i + 1;
      for (; j < fragments.size(); j++) {
        var target = fragments.get(j);
        if (!Fragment.overlaps(source, target)) {
          break;
        }
      }

      if (j == i + 1) {
        filteredFragments.add(source);
        i++;
      } else {
        int best = pickBestFragment(fragments.subList(i, j));
        filteredFragments.add(fragments.get(i + best));
        i = j;
      }
    }

    return filteredFragments;
  }

  private int pickBestFragment(List<Fragment> overlapping) {
    var pivot = overlapping.get(0);
    int maxi = 0;
    double maxSim = Double.MIN_VALUE;
    for (int i = 1; i < overlapping.size(); i++) {
      var other = overlapping.get(i);
      var overlappedSents = pivot.intersect(other);
      var overlapToPivotComplement = pivot.complement(other);
      var overlapToOtherComplement = other.complement(pivot);

      double pivotFragmentQuality = fragmentQuality(
        pivot.copy(), overlappedSents, overlapToPivotComplement
      );
      double otherFragmentQuality = fragmentQuality(other.copy(), overlappedSents, overlapToOtherComplement);
      if (pivotFragmentQuality > otherFragmentQuality) {
        if (pivotFragmentQuality > maxSim) {
          maxi = 0;
          maxSim = pivotFragmentQuality;
        }
      }

      if (otherFragmentQuality > pivotFragmentQuality) {
        if (otherFragmentQuality > maxSim) {
          maxi = i;
          maxSim = otherFragmentQuality;
        }
      }
    }

    return maxi;
  }

  /**
   * Computes fragment quality considering overlapped part "fitness" and complement "fitness"
   *
   * @implNote Fragment will be sorted by second document
   */
  private double fragmentQuality(Fragment fragment,
                                 List<CoreSentence> overlap,
                                 List<CoreSentence> complement) {
    fragment.sortSeedsBySideIndex(1);
    var secondCoreDocument = fragment.getSeeds().get(0).getSecondSent().document();

    int secondStartIndex = fragment.getStartSideIndex(1);
    int secondEndIndex = fragment.getEndSideIndex(1);
    var secondDocumentSentences = secondCoreDocument.sentences()
      .subList(secondStartIndex, secondEndIndex + 1);

    double overlapToFragmentSim = fragmentSimilarityAsymmetric(overlap, secondDocumentSentences);
    double complementToFragmentSim = fragmentSimilarityAsymmetric(complement, secondDocumentSentences);

    return overlapToFragmentSim + (1 - overlapToFragmentSim) * complementToFragmentSim;
  }

  /**
   * Compares first fragment to the second using average bestMatch
   */
  private double fragmentSimilarityAsymmetric(List<CoreSentence> firstFragment,
                                              List<CoreSentence> secondFragment) {

    double bestMatchSum = 0.0;
    for (var firstSent : firstFragment) {
      var firstSentAccVector = embeddings.getAccVector(firstSent);
      double maxSim = Double.NaN;
      for (var secondSent : secondFragment) {
        var secondSentAccVector = embeddings.getAccVector(secondSent);
        double sim = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);
        if (Double.isNaN(maxSim)) {
          maxSim = sim;
        } else if (!Double.isNaN(sim)) {
          maxSim = Math.max(maxSim, sim);
        }
      }

      if (!Double.isNaN(maxSim)) {
        bestMatchSum += maxSim;
      }
    }

    return bestMatchSum / secondFragment.size();
  }

}
