package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.*;

@Slf4j
@AllArgsConstructor
public class SeedingStrategy {

  Embeddings embeddings;

  double diceSimilarityThreshold;

  double sentenceVectorSimilarityThreshold;

  @NotNull
  public List<int[]> seed(@NotNull List<List<CoreMap>> documents) {
    var seeds = new ArrayList<int[]>();
    var firstSents = documents.get(0);
    var secondSents = documents.get(1);
    log.trace("First document contains {} sentences", firstSents.size());
    log.trace("Second document contains {} sentences", secondSents.size());
    log.trace("Possible seeds count {}", documents.get(0).size() * firstSents.size());

    var firstSentVectors = firstSents.stream()
      .map(embeddings::getAccVector)
      .toList();

    var secondSentVectors = secondSents.stream()
      .map(embeddings::getAccVector)
      .toList();

    int firstSize = firstSents.size();
    int secondSize = secondSents.size();

    for (int i = 0; i < firstSize; i++) {
      var firstSent = firstSents.get(i);
      var firstSentAccVector = firstSentVectors.get(i);

      for (int j = 0; j < secondSize; j++) {
        var secondSent = secondSents.get(j);

        var diceSim = diceCoefficient(firstSent, secondSent);
        if (diceSim < diceSimilarityThreshold) {
          continue;
        }

        var secondSentAccVector = secondSentVectors.get(j);
        var cosineSim = Transforms.cosineSim(firstSentAccVector, secondSentAccVector);

        if (!Double.isNaN(cosineSim) && cosineSim > sentenceVectorSimilarityThreshold) {
          log.trace(
            "Seed:\n\tS1 [{}] {}\n\tS2 [{}] {}\n\tCosine {}\n\tDice {}\n\n",
            i, AnnotationUtils.tokensText(firstSent), j, AnnotationUtils.tokensText(secondSent), cosineSim, diceSim
          );
          seeds.add(new int[]{i, j});
        }
      }
    }

    return seeds;
  }

  public static double diceCoefficient(
    @NotNull CoreMap firstSentence,
    @NotNull CoreMap secondSentence
  ) {
    var firstLemmaSet = new HashSet<>(AnnotationUtils.lemmas(firstSentence));
    var secondLemmaSet = new HashSet<>(AnnotationUtils.lemmas(secondSentence));
    double intersectionSize = Sets.intersection(firstLemmaSet, secondLemmaSet).size() * 1.0;
    return 2 * intersectionSize / (firstLemmaSet.size() + secondLemmaSet.size());
  }
}
