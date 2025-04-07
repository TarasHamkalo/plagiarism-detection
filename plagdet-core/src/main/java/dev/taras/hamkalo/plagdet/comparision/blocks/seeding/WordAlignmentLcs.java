package dev.taras.hamkalo.plagdet.comparision.blocks.seeding;

import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.*;

@Slf4j
@AllArgsConstructor
public class WordAlignmentLcs implements SeedingStrategy {

  Embeddings embeddings;

  double similarityThreshold;

  @Override
  public @NotNull List<int[]> seed(@NotNull List<List<CoreMap>> documents) {
    var seeds = new ArrayList<int[]>();
    var firstSents = documents.get(0);
    var secondSents = documents.get(1);

    for (int i = 0; i < firstSents.size(); i++) {
      var firstSentence = firstSents.get(i);
      var firstWordVectors = embeddings.getWordVectorsList(firstSentence);

      for (int j = 0; j < secondSents.size(); j++) {
        var secondSentence = secondSents.get(j);
        var secondWordVectors = embeddings.getWordVectorsList(secondSentence);

        int lcsLength = longestCommonSubsequence(firstWordVectors, secondWordVectors);

        if (lcsLength > 2) { // Require at least 3-word contiguous match
          log.trace("Seed Match: S1[{}] <-> S2[{}], LCS length: {}", i, j, lcsLength);
          seeds.add(new int[]{i, j});
        }
      }
    }
    return seeds;
  }

  private int longestCommonSubsequence(List<INDArray> vec1, List<INDArray> vec2) {
    int m = vec1.size(), n = vec2.size();
    int[][] dp = new int[m + 1][n + 1];

    for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
        double sim = Transforms.cosineSim(vec1.get(i - 1), vec2.get(j - 1));
        if (sim > similarityThreshold) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }
    return dp[m][n];
  }
}
