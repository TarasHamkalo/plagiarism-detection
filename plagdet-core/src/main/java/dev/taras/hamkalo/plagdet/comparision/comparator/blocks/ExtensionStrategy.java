/**
* This class implements extension strategy of the plagiarism detection algorithm
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
import dev.taras.hamkalo.plagdet.comparision.Seed;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExtensionStrategy {

  Embeddings embeddings;

  double minFragmentSideSim;

  int minClusterSize;

  public List<Fragment> extend(List<Seed> seeds, int sentMaxGap) {
    var fragments = clustering(seeds, sentMaxGap, 0, true);
    fragments = validation(fragments, sentMaxGap);
    return fragments;
  }

  /**
   * Split fragment each time it sides have low similarity score
   *
   * @return List of fragment with having similarity score at least MIN_CLUSTER_SIDE_SIM
   */
  private @NotNull List<Fragment> validation(@NotNull List<Fragment> fragments, int sentMaxGap) {
    List<Fragment> newFragments = new ArrayList<>();
    for (var fragment : fragments) {
      var sideSim = sideSimilarity(fragment);
      if (sideSim < minFragmentSideSim) {
        log.warn("Sim is {}, so splitting fragment with {} seeds", sideSim, fragment.getSeeds().size());
        if (sentMaxGap > 0) {
          newFragments.addAll(extend(fragment.getSeeds(), sentMaxGap - 1));
        }
      } else {
        newFragments.add(fragment);
      }
    }

    return newFragments;
  }

  private double sideSimilarity(@NotNull Fragment fragment) {
    var firstAccVector = embeddings.getSideAccVector(fragment, 0);
    var secondAccVector = embeddings.getSideAccVector(fragment, 1);

    return Transforms.cosineSim(firstAccVector, secondAccVector);
  }

  /**
   * Merges seeds into continuous fragments, considering only one side of document pair
   *
   * @return list of merged seeds
   */
  private @NotNull List<Fragment> clusterOneSide(@NotNull List<Seed> seeds, int sentMaxGap, int side) {
    seeds.sort(Comparator.comparingInt(s -> s.getSideIndex(side)));

    var clusters = new ArrayList<Fragment>();
    var cluster = new ArrayList<Seed>();
    for (Seed seed : seeds) {
      if (cluster.isEmpty()) {
        cluster.add(seed);
        continue;
      }

      var lastSeed = cluster.get(cluster.size() - 1);
      if (lastSeed.isAdjacent(seed, side, sentMaxGap)) {
        cluster.add(seed);
      } else {
        if (cluster.size() >= minClusterSize) {
          clusters.add(Fragment.builder().seeds(new ArrayList<>(cluster)).build());
        }
        cluster.clear();
        cluster.add(seed);
      }
    }

    if (cluster.size() >= minClusterSize) {
      clusters.add(Fragment.builder().seeds(new ArrayList<>(cluster)).build());
    }

    return clusters;
  }

  private List<Fragment> clustering(List<Seed> seeds, int sentMaxGap, int side, boolean firstIter) {
    var clusters = clusterOneSide(seeds, sentMaxGap, side);
    if (clusters.size() <= 1 && !firstIter) {
      return clusters;
    }

    List<Fragment> newClusters = new ArrayList<>();
    for (var cluster : clusters) {
      newClusters.addAll(clustering(cluster.getSeeds(), sentMaxGap, side + 1, false));
    }

    return newClusters;
  }

}
