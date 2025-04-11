package dev.taras.hamkalo.plagdet.indexing.jvector;


import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import io.github.jbellis.jvector.graph.*;
import io.github.jbellis.jvector.graph.similarity.BuildScoreProvider;
import io.github.jbellis.jvector.graph.similarity.SearchScoreProvider;
import io.github.jbellis.jvector.util.Bits;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SentenceIndex implements AutoCloseable {

  private static final VectorTypeSupport vectorTypeSupport = VectorizationProvider.getInstance().getVectorTypeSupport();

  @NotNull Embeddings embeddings;

  @NotNull List<VectorFloat<?>> baseVectors;

  @NotNull List<int[]> sentenceDocumentPairs;

  @NotNull List<CoreMap> globalSentencesList;

  @NotNull OnHeapGraphIndex index;

  @NotNull GraphSearcher graphSearcher;

  @NotNull ListRandomAccessVectorValues listRandomAccess;

  private SentenceIndex(
    @NotNull Embeddings embeddings,
    @Nullable List<VectorFloat<?>> baseVectors,
    @Nullable List<int[]> sentenceDocumentPairs,
    @NotNull List<CoreMap> globalSentencesList
  ) {
    int originalDimension = baseVectors.get(0).length();
    var ravv = new ListRandomAccessVectorValues(baseVectors, originalDimension);
    var bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.COSINE);
    log.trace("Building index");
    try (var builder = new GraphIndexBuilder(bsp, ravv.dimension(), 16, 100, 1.2f, 1.2f)) {
      this.index = builder.build(ravv);
      this.graphSearcher = new GraphSearcher(index);
      this.listRandomAccess = new ListRandomAccessVectorValues(
        baseVectors, baseVectors.get(0).length()
      );
      this.baseVectors = baseVectors;
      this.embeddings = embeddings;
      this.sentenceDocumentPairs = sentenceDocumentPairs;
      this.globalSentencesList = globalSentencesList;
      log.trace("Building index done");
    } catch (IOException e) {
      throw new PlagdetException("Failed to build sentence index", e);
    }
  }

  public static @NotNull SentenceIndex from(
    @NotNull Embeddings embeddings,
    @NotNull List<DocumentEntry> documents
  ) throws PlagdetException {
    List<VectorFloat<?>> baseVectors = new ArrayList<>();
    List<int[]> sentenceDocumentPairs = new ArrayList<>();
    List<CoreMap> globalSentencesList = new ArrayList<>();

    log.trace("Querying embeddings");
    int documentsSize = documents.size();
    int globalSentenceIndex = 0;
    for (int i = 0; i < documentsSize; i++) {
      var doc = documents.get(i);
      var sentences = AnnotationUtils.sentences(doc.getAnnotation());
      int sentencesSize = sentences.size();
      for (int j = 0; j < sentencesSize; j++) {
        baseVectors.add(vectorTypeSupport.createFloatVector(
          embeddings.getAccVector(sentences.get(j)).toFloatVector())
        );
        sentenceDocumentPairs.add(new int[]{i, j, globalSentenceIndex});
        globalSentencesList.add(sentences.get(j));
        globalSentenceIndex++;
      }
    }

    log.trace("Querying embeddings done");
    embeddings.clearSentenceVectorCache();

    return new SentenceIndex(embeddings, baseVectors, sentenceDocumentPairs, globalSentencesList);
  }

  public void clearGlobalSentenceList() {
    this.globalSentencesList.clear();
  }

  public @NotNull CoreMap findSentenceByGlobalIndex(int globalSentenceIndex) {
    return globalSentencesList.get(globalSentenceIndex);
  }

  public List<int[]> findSimilar(int globalSentenceIndex, int topK, float th) {
    return findSimilar(baseVectors.get(globalSentenceIndex), topK, th);
  }

  public List<int[]> findSimilar(CoreMap querySentence, int topK, float th) {
    var queryVec = embeddings.getAccVector(querySentence).toFloatVector();
    var query = vectorTypeSupport.createFloatVector(queryVec);
    return findSimilar(query, topK, th);
  }

  public List<int[]> findSimilar(VectorFloat<?> query, int topK, float th) {
    var search = SearchScoreProvider.exact(
      query, VectorSimilarityFunction.COSINE, listRandomAccess
    );
    SearchResult sr = graphSearcher.search(search, topK, th, Bits.ALL);
    List<int[]> matches = new ArrayList<>();
    for (SearchResult.NodeScore ns : sr.getNodes()) {
      matches.add(sentenceDocumentPairs.get(ns.node));
    }
    return matches;
  }

  @Override
  public void close() throws IOException {
    this.graphSearcher.close();
    this.clearGlobalSentenceList();
  }
}

//  public List<int[]> inMemoryIndexLookup(
//    List<CoreMap> firstSents,
//    List<CoreMap> secondSents,
//    List<VectorFloat<?>> baseVectors
//  ) throws IOException {
//
//    List<int[]> seeds = new ArrayList<>();
//    int originalDimension = baseVectors.get(0).length();
//    RandomAccessVectorValues ravv = new ListRandomAccessVectorValues(baseVectors, originalDimension);
//    BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.COSINE);
//    int discarded = 0;
//    int scored = 0;
//    try (GraphIndexBuilder builder = new GraphIndexBuilder(bsp, ravv.dimension(), 16, 100, 1.2f, 1.2f);
//         OnHeapGraphIndex index = builder.build(ravv);
//         GraphSearcher graphSearcher = new GraphSearcher(index)) {
//      System.out.println("Index was created");
//      for (int i = 0; i < firstSents.size(); i++) {
//        var queryVec = embeddings.getAccVector(firstSents.get(i)).toFloatVector();
//        var query = vectorTypeSupport.createFloatVector(queryVec);
//        System.out.println("Querying");
/// /        GraphSearcher.search();
/// /          public SearchResult search(SearchScoreProvider scoreProvider,
/// /                               int topK,
/// /                               float threshold,
/// /                               Bits acceptOrds) {
//
/// /        SearchResult sr = GraphSearcher.search(
/// /          query,
/// /          10, // top-k
/// /          ravv,
/// /          VectorSimilarityFunction.COSINE,
/// /          index,
/// /          Bits.ALL
/// /        );
//        SearchResult sr = graphSearcher.search(
//          SearchScoreProvider.exact(query, VectorSimilarityFunction.COSINE, ravv),
//          20 * 1000,
//          0.7f,
//          Bits.ALL
//        );
//        scored += sr.getNodes().length;
//        System.out.println("results count " + sr.getNodes().length);
//        for (SearchResult.NodeScore ns : sr.getNodes()) {
//          double sim = computeWeightedSim(firstSents.get(i), secondSents.get(ns.node));
//          if (sim > th) {
/// /            log.trace("Accepted alignment {}", sim);
//            seeds.add(new int[]{i, ns.node});
//          } else {
//            discarded++;
//          }
//        }
//      }
//    }
//
//    log.trace("All {}, Scored  {}, Discarded count {}", firstSents.size() * secondSents.size(), scored, discarded);
//    return seeds;
//  }
//}
