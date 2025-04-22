package dev.taras.hamkalo.plagdet.indexing.jvector;


import dev.taras.hamkalo.plagdet.comparision.Embeddings;
import dev.taras.hamkalo.plagdet.documents.corpus.Corpus;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.pipeline.Annotation;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SentenceIndex implements AutoCloseable {

  private static final VectorTypeSupport VECTOR_TYPE_SUPPORT =
    VectorizationProvider.getInstance().getVectorTypeSupport();

  @NotNull Embeddings embeddings;

  @NotNull List<VectorFloat<?>> baseVectors;

  @NotNull List<int[]> sentenceDocumentPairs;

  @NotNull List<CoreMap> globalSentencesList;

  @NotNull OnHeapGraphIndex index;

  @NotNull GraphSearcher graphSearcher;

  @NotNull ListRandomAccessVectorValues listRandomAccess;

  private SentenceIndex(
    @NotNull Embeddings embeddings,
    @NotNull List<VectorFloat<?>> baseVectors,
    @NotNull List<int[]> sentenceDocumentPairs,
    @NotNull List<CoreMap> globalSentencesList
  ) {
    int originalDimension = baseVectors.isEmpty() ? 1 : baseVectors.get(0).length();
    var ravv = new ListRandomAccessVectorValues(baseVectors, originalDimension);
    var bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.COSINE);
    log.trace("Building index");
    try (var builder = new GraphIndexBuilder(bsp, ravv.dimension(), 16, 100, 1.2f, 1.2f)) {
      this.index = builder.build(ravv);
      this.graphSearcher = new GraphSearcher(index);
      this.listRandomAccess = ravv;
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
    @NotNull Corpus corpus
  ) {
    List<VectorFloat<?>> baseVectors = new ArrayList<>();
    List<int[]> sentenceDocumentPairs = new ArrayList<>();
    List<CoreMap> globalSentencesList = new ArrayList<>();
    log.trace("Querying embeddings");
    int globalSentenceIndex = 0;
    int i = 0;
    while (corpus.hasNext()) {
      var doc = corpus.next();
      var sentences = AnnotationUtils.sentences(doc.getAnnotation());
      int sentencesSize = sentences.size();
      for (int j = 0; j < sentencesSize; j++) {
        CoreMap sentence = sentences.get(j);
        baseVectors.add(VECTOR_TYPE_SUPPORT.createFloatVector(
          embeddings.getAccVector(sentence).toFloatVector())
        );
        sentenceDocumentPairs.add(
          new int[]{i, j, globalSentenceIndex, AnnotationUtils.textBlockId(sentence)}
        );
        globalSentencesList.add(sentence);
        globalSentenceIndex++;
      }

      i++;
    }

    corpus.reset();
    log.trace("Querying embeddings done");
    embeddings.clearSentenceVectorCache();
    return new SentenceIndex(embeddings, baseVectors, sentenceDocumentPairs, globalSentencesList);
  }

  public static @NotNull SentenceIndex from(
    @NotNull Embeddings embeddings,
    @NotNull Annotation annotation
  ) throws PlagdetException {
    List<VectorFloat<?>> baseVectors = new ArrayList<>();
    List<int[]> sentenceDocumentPairs = new ArrayList<>();
    List<CoreMap> globalSentencesList = new ArrayList<>();
    log.trace("Querying embeddings");
    int globalSentenceIndex = 0;
    var sentences = AnnotationUtils.sentences(annotation);
    int sentencesSize = sentences.size();
    for (int j = 0; j < sentencesSize; j++) {
      CoreMap sentence = sentences.get(j);
      baseVectors.add(VECTOR_TYPE_SUPPORT.createFloatVector(
        embeddings.getAccVector(sentence).toFloatVector())
      );
      sentenceDocumentPairs.add(
        new int[]{0, j, globalSentenceIndex, AnnotationUtils.textBlockId(sentence)}
      );
      globalSentencesList.add(sentence);
      globalSentenceIndex++;
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

  public @NotNull VectorFloat<?> findSentenceVectorByGlobalIndex(int globalSentenceIndex) {
    return baseVectors.get(globalSentenceIndex);
  }

  public @NotNull List<int[]> findSimilar(int globalSentenceIndex, int topK, float th) {
    return findSimilar(baseVectors.get(globalSentenceIndex), topK, th);
  }

  public @NotNull List<int[]> findSimilar(@NotNull CoreMap querySentence, int topK, float th) {
    var queryVec = embeddings.getAccVector(querySentence).toFloatVector();
    var query = VECTOR_TYPE_SUPPORT.createFloatVector(queryVec);
    return findSimilar(query, topK, th);
  }

  public @NotNull List<int[]> findSimilar(@NotNull VectorFloat<?> query, int topK, float th) {
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