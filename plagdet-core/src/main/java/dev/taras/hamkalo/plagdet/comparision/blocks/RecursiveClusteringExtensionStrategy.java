/**
 * This class implements extension strategy of the plagiarism detection algorithm
 * described in the following papers:
 * <p>
 * 1. Sanchez-Perez, M.A., Gelbukh, A., Sidorov, G.:
 * Adaptive algorithm for plagiarism detection: The best-performing approach at PAN 2014 text alignment competition.
 * Lecture Notes in Computer Science, vol. 9283, Springer, 2015, pp. 402-413.
 * <p>
 * 2. Sanchez-Perez, M.A., Gelbukh, A.F., Sidorov, G.:
 * Dynamically adjustable approach through obfuscation type recognition.
 * Working Notes of CLEF 2015 - Conference and Labs of the Evaluation forum, Toulouse, France, September 8-11, 2015.
 * CEUR Workshop Proceedings, vol. 1391, CEUR-WS.org, 2015, http://ceur-ws.org/Vol-1391/92-CR.pdf.
 * <p>
 * Original source: https://www.gelbukh.com/plagiarism-detection/PAN-2015/index.html
 * License: Free for non-commercial academic purposes.
 */

package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.util.SeedUtils;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecursiveClusteringExtensionStrategy implements ExtensionStrategy {

  @Value
  @Builder
  public static class RecursiveClusteringExtensionStrategyParameters {

    /**
     * Cosine similarity threshold for considering two clusters' sides similar.
     */
    public static final double DEFAULT_MIN_FRAGMENT_SIDE_SIM = 0.9;

    /**
     * The small cluster that will be recorded, less than this threshold are dropped
     */
    public static final int DEFAULT_MIN_CLUSTER_SIZE = 4;

    /**
     * Gap between two matches which is acceptable during match merging
     */
    public static final int DEFAULT_SENT_MAX_GAP = 3;

    @Builder.Default
    double minFragmentSideSim = DEFAULT_MIN_FRAGMENT_SIDE_SIM;

    @Builder.Default
    int minClusterSize = DEFAULT_MIN_CLUSTER_SIZE;

    @Builder.Default
    int sentMaxGap = DEFAULT_SENT_MAX_GAP;
  }


  @NotNull Embeddings embeddings;

  @NotNull RecursiveClusteringExtensionStrategyParameters parameters;

  @Override
  public @NotNull List<Fragment> extend(@NotNull DocumentPool documentPool, @NotNull List<int[]> seeds) {
    var documents = List.of(
      AnnotationUtils.sentences(documentPool.getSuspiciousDocument().getAnnotation()),
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation())
    );
    return extend(documents, seeds, parameters.sentMaxGap);
  }

  @Override
  public @NotNull List<Fragment> extend(@NotNull List<List<CoreMap>> documents, @NotNull List<int[]> seeds) {
    return extend(documents, seeds, parameters.sentMaxGap);
  }

  public @NotNull List<Fragment> extend(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<int[]> seeds,
    int sentMaxGap
  ) {
    if (seeds.isEmpty()) {
      return Collections.emptyList();
    }
    Fragment root = new Fragment(seeds);
    return extend(documents, root, sentMaxGap);
  }

  private @NotNull List<Fragment> extend(
    @NotNull List<List<CoreMap>> documents,
    @NotNull Fragment root,
    int sentMaxGap
  ) {
    var fragments = clustering(root, sentMaxGap, 0, true);
    fragments = validation(documents, fragments, sentMaxGap);
    return fragments;
  }

  /**
   * Split fragment each time it sides have low similarity score
   *
   * @return List of fragment with having similarity score at least MIN_CLUSTER_SIDE_SIM
   */
  private @NotNull List<Fragment> validation(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<Fragment> fragments,
    int sentMaxGap
  ) {
    List<Fragment> newFragments = new ArrayList<>();
    for (var fragment : fragments) {
      var sideSim = sideSimilarity(documents, fragment);
      if (sideSim < parameters.minFragmentSideSim) {
        log.warn("Sim is {}, so splitting fragment with {} seeds", sideSim, fragment.getSeeds(0).size());
        if (sentMaxGap > 0) {
          newFragments.addAll(extend(documents, fragment, sentMaxGap - 1));
        }
      } else {
        newFragments.add(fragment);
      }
    }

    return newFragments;
  }

  private double sideSimilarity(@NotNull List<List<CoreMap>> documents, @NotNull Fragment fragment) {
    int[] starts = fragment.getSideStarts();
    int[] ends = fragment.getSideEnds();
    var firstAccVector = embeddings.getAccVector(documents.get(0), starts[0], ends[0]);
    var secondAccVector = embeddings.getAccVector(documents.get(1), starts[1], ends[1]);
    return Transforms.cosineSim(firstAccVector, secondAccVector);
  }

  /**
   * Merges seeds into continuous fragments, considering only one side of document pair
   *
   * @return list of merged seeds
   */
  private @NotNull List<Fragment> clusterOneSide(
    @NotNull Fragment cluster,
    int sentMaxGap,
    int side
  ) {
    var seeds = cluster.getSeeds(side);
    if (seeds.isEmpty()) {
      return Collections.emptyList();
    }

    var clusters = new ArrayList<Fragment>();
    int seedsSize = seeds.size();
    int clusterStart = 0;
    int clusterEnd = 0;
    //TODO: check for off by one
    for (int i = 1; i < seedsSize; i++) {
      if (SeedUtils.isAdjacent(seeds.get(clusterEnd), seeds.get(i), side, sentMaxGap)) {
        clusterEnd++;
        continue;
      }

      int[] startSeed = seeds.get(clusterStart);
      int[] endSeed = seeds.get(clusterEnd);

      if ((endSeed[side] - startSeed[side] + 1) >= parameters.minClusterSize) {
        clusters.add(new Fragment(cluster, clusterStart, clusterEnd, side));
      }

      clusterStart = i;
      clusterEnd = clusterStart;
    }

    int[] startSeed = seeds.get(clusterStart);
    int[] endSeed = seeds.get(clusterEnd);
    if ((endSeed[side] - startSeed[side] + 1) >= parameters.minClusterSize) {
      clusters.add(new Fragment(cluster, clusterStart, clusterEnd, side));
    }
    return clusters;
  }

  private @NotNull List<Fragment> clustering(
    @NotNull Fragment root,
    int sentMaxGap,
    int side,
    boolean firstIter
  ) {
    var clusters = clusterOneSide(root, sentMaxGap, side % 2);
    if (clusters.size() <= 1 && !firstIter) {
      return clusters;
    }

    List<Fragment> newClusters = new ArrayList<>();
    for (var cluster : clusters) {
      newClusters.addAll(clustering(cluster, sentMaxGap, side + 1, false));
    }

    return newClusters;
  }

}
