package dev.taras.hamkalo.plagdet.comparision.blocks.seeding;//import com..oss.simulacron.common.cluster.ClusterSpec;

import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import edu.stanford.nlp.util.CoreMap;
import io.github.jbellis.jvector.graph.*;
import io.github.jbellis.jvector.graph.similarity.BuildScoreProvider;
import io.github.jbellis.jvector.graph.similarity.SearchScoreProvider;
import io.github.jbellis.jvector.util.Bits;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class HybridSeedingStrategy implements SeedingStrategy {

  private static final VectorTypeSupport vectorTypeSupport = VectorizationProvider.getInstance().getVectorTypeSupport();

  Embeddings embeddings;

  double th;

  @Override
  public @NotNull List<int[]> seed(@NotNull List<List<CoreMap>> documents) {
    var firstSents = documents.get(0);
    var secondSents = documents.get(1);
    var secondVectors = secondSents.stream()
      .map(embeddings::getAccVector)
      .map(v -> v.toFloatVector())
      .toList();

    ArrayList<VectorFloat<?>> baseVectors = new ArrayList<>();
    secondVectors.stream().map(v -> vectorTypeSupport.createFloatVector(v)).forEach(v -> baseVectors.add(v));

    try {
      return inMemoryIndexLookup(firstSents, secondSents, baseVectors);
    } catch (IOException e) {
      throw new RuntimeException("Failed to build or query JVector index", e);
    }
  }

  public List<int[]> inMemoryIndexLookup(
    List<CoreMap> firstSents,
    List<CoreMap> secondSents,
    List<VectorFloat<?>> baseVectors
  ) throws IOException {

    List<int[]> seeds = new ArrayList<>();
    int originalDimension = baseVectors.get(0).length();
    RandomAccessVectorValues ravv = new ListRandomAccessVectorValues(baseVectors, originalDimension);
    BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.COSINE);
    int discarded = 0;
    int scored = 0;
    try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, ravv.dimension(), 16, 100, 1.2f, 1.2f);
         OnHeapGraphIndex index = builder.build(ravv);
         GraphSearcher graphSearcher = new GraphSearcher(index)) {
      System.out.println("Index was created");
      for (int i = 0; i < firstSents.size(); i++) {
        var queryVec = embeddings.getAccVector(firstSents.get(i)).toFloatVector();
        var query = vectorTypeSupport.createFloatVector(queryVec);
        System.out.println("Querying");
//        GraphSearcher.search();
//          public SearchResult search(SearchScoreProvider scoreProvider,
//                               int topK,
//                               float threshold,
//                               Bits acceptOrds) {

//        SearchResult sr = GraphSearcher.search(
//          query,
//          10, // top-k
//          ravv,
//          VectorSimilarityFunction.COSINE,
//          index,
//          Bits.ALL
//        );
        SearchResult sr = graphSearcher.search(
          SearchScoreProvider.exact(query, VectorSimilarityFunction.COSINE, ravv),
          20 * 1000,
          0.7f,
          Bits.ALL
        );
        scored += sr.getNodes().length;
        System.out.println("results count " + sr.getNodes().length);
        for (SearchResult.NodeScore ns : sr.getNodes()) {
          double sim = computeWeightedSim(firstSents.get(i), secondSents.get(ns.node));
          if (sim > th) {
//            log.trace("Accepted alignment {}", sim);
            seeds.add(new int[]{i, ns.node});
          } else {
            discarded++;
          }
        }
      }
    }

    log.trace("All {}, Scored  {}, Discarded count {}", firstSents.size() * secondSents.size(), scored, discarded);
    return seeds;
  }

  private double computeWeightedSim(CoreMap sent1, CoreMap sent2) {
    var s1Vecs = embeddings.getWordVectorsWithWeights(sent1);
    var s2Vecs = embeddings.getWordVectorsWithWeights(sent2);

    var vec1 = s1Vecs.first();
    var vec2 = s2Vecs.first();

    if (vec1.isEmpty() || vec2.isEmpty()) return 0;

    var w1 = s1Vecs.second().reshape(s1Vecs.second().size(0), 1);
    var w2 = s2Vecs.second().reshape(s2Vecs.second().size(0), 1);

    var simMatrix = WeightedWordAlignment.cosineMatrix(vec1, vec2);
    if (simMatrix == null) return 0;

    var max1 = simMatrix.max(1);
    var max2 = simMatrix.max(0);

    double weighted1 = max1.mmul(w1).getDouble(0) / w1.sumNumber().doubleValue();
    double weighted2 = max2.mmul(w2).getDouble(0) / w2.sumNumber().doubleValue();

    return 0.5 * (weighted1 + weighted2) *
           Math.min((double) w1.size(0) / w2.size(0), (double) w2.size(0) / w1.size(0));
  }
}
