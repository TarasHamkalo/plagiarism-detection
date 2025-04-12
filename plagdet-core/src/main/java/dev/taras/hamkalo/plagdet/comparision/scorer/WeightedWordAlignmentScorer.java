package dev.taras.hamkalo.plagdet.comparision.scorer;

import edu.stanford.nlp.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

public class WeightedWordAlignmentScorer {

  public static boolean areSimilar(
    @NotNull Pair<INDArray, INDArray> firstSent,
    @NotNull Pair<INDArray, INDArray> secondSent,
    double sizeRatioThreshold,
    double similarityThreshold
  ) {
    INDArray w1 = firstSent.second();
    INDArray w2 = secondSent.second();
    double sizeRatio = Math.min(
      (double) w1.length() / w2.length(),
      (double) w2.length() / w1.length()
    );

    if (sizeRatio < sizeRatioThreshold) {
      return false;
    }

    INDArray cosineMatrix = cosineMatrix(firstSent.first(), secondSent.first());
    if (cosineMatrix == null) {
      return false;
    }

    INDArray max1 = cosineMatrix.max(1);
    INDArray max2 = cosineMatrix.max(0);

    double sim1 = max1.mmul(w1).getDouble(0) / w1.sum(0).getDouble(0);
    double sim2 = max2.mmul(w2).getDouble(0) / w2.sum(0).getDouble(0);

    double similarity = 0.5 * (sim1 + sim2);
    return similarity > similarityThreshold;
  }

  public static @Nullable INDArray cosineMatrix(
    @NotNull INDArray A,
    @NotNull INDArray B
  ) {
    return (A.isEmpty() || B.isEmpty()) ? null : Transforms.allCosineSimilarities(A, B, 1);
  }
}
