package dev.taras.hamkalo.plagdet.comparision.blocks.seeding;

import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.*;

@Slf4j
@AllArgsConstructor
public class WeightedWordAlignment implements SeedingStrategy {

  Embeddings embeddings;

  double th;

  @Override
  public @NotNull List<int[]> seed(@NotNull List<List<CoreMap>> documents) {
    var seeds = new ArrayList<int[]>();
    var firstSents = documents.get(0);
    var secondSents = documents.get(1);
    int firstSize = firstSents.size();
    int secondSize = secondSents.size();

    log.trace("First document contains {} sentences", firstSents.size());
    log.trace("Second document contains {} sentences", secondSents.size());
    log.trace("Possible seeds count {}", documents.get(0).size() * firstSents.size());

    var firstSentVectors = firstSents.stream()
      .map(embeddings::getWordVectorsWithWeights)
      .toList();

    var secondSentVectors = secondSents.stream()
      .map(embeddings::getWordVectorsWithWeights)
      .toList();

    for (int i = 0; i < firstSize; i++) {
      var firstSent = firstSentVectors.get(i);
//      var firstWeights = firstSent.second().transpose();
      INDArray firstWeights = firstSent.second().reshape(firstSent.second().size(0), 1);

      for (int j = 0; j < secondSize; j++) {
        var secondSent = secondSentVectors.get(j);
        var secondWeights = secondSent.second().reshape(secondSent.second().size(0), 1);
        var cosineMatrix = cosineMatrix(firstSent.first(), secondSent.first());
        if (cosineMatrix == null) {
//          log.trace(
//            "Skipped, null best match cosines:\n\tS1 [{}] {}\n\tS2 [{}] {}\n\n",
//            i, AnnotationUtils.tokensText(firstSents.get(i)), j, AnnotationUtils.tokensText(secondSents.get(j))
//          );

          continue;
        }
        var firstMatches = cosineMatrix.max(1);
        var secondMatches = cosineMatrix.max(0);
        var weightAndCosineFirstSum = firstMatches.mmul(firstWeights);
        var weightAndCosineSecondSum = secondMatches.mmul(secondWeights);
//        double sim = 0.5 * (weightAndCosineFirstSum.get(0) / firstWeights.sum(0) + weightAndCosineSecondSum.get(0) / secondWeights.sum(0));
        double sim = 0.5 * (weightAndCosineFirstSum.getDouble(0) / firstWeights.sumNumber().doubleValue() +
                            weightAndCosineSecondSum.getDouble(0) / secondWeights.sumNumber().doubleValue());

        sim *= Math.min((double) firstWeights.size(0) / secondWeights.size(0), (double) secondWeights.size(0) / firstWeights.size(0));
        log.trace(
          "Seed:\n\tS1 [{}] {}\n\tS2 [{}] {}\n\tSim [{}]\n\n",
          i, AnnotationUtils.tokensText(firstSents.get(i)), j, AnnotationUtils.tokensText(secondSents.get(j)),
          sim
        );

        if (sim > th) {
          seeds.add(new int[]{i, j});
        }
      }
    }

    log.trace("Seeds count {}", seeds.size());
    return seeds;
  }

//  private Map<String, Double> computeFrequencies(Map<String, Double> frequencyMap, CoreMap first, CoreMap second) {
//    var idfMap = new HashMap<String, Double>();
//    var firstTokens = AnnotationUtils.wordTokens(first);
//    for (int i = 0; i < firstTokens.size(); i++) {
//      var frequency = idfMap.get(firstTokens.get(i).word());
//    }
//  }
//
  public INDArray cosineMatrix(INDArray A, INDArray B) {

    if (A.isEmpty() || B.isEmpty()) {
      return null;
    }
    return Transforms.allCosineSimilarities(A, B, 1);
  }

}
