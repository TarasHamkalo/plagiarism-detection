package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.SpecialMarking;
import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.NopExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.util.ScoreUtils;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.jvector.SentenceIndex;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import javax.swing.text.Document;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndexedSentenceComparator {

  public static final float SENTENCE_VECTOR_SIMILARITY_THRESHOLD = 0.7f;

  public static final float SIMILARITY_THRESHOLD = 0.65f;

  @NotNull Embeddings embeddings;

  /**
   * Index corresponds to indexed documents collection index
   * HashMap keys also correspond to index of document in indexed collection
   */
  @NotNull
  List<HashMap<Integer, List<int[]>>> documentToMatchGroups = new ArrayList<>();

  /**
   * Caches sentence word matrices for each document in collection
   * Sentence global list, cached in sentence index is flat sentence collection for fast lookup
   * The representation stored here, is matrix and weights for each sentence,
   * goal is to free it aftere document was compared, so it will be more
   * efficient to store organized
   */
  @NotNull
  Map<Integer, Map<Integer, Pair<INDArray, INDArray>>> docToSentenceVectors = new HashMap<>();

  /**
   * Stores doc to doc (collection index) saying whether that comparisons is already done
   */
  @NotNull
  Set<String> comparisonsDone = new HashSet<>();

  public @NotNull List<DocumentPair> compare(@NotNull List<DocumentEntry> documentEntries) {
    log.trace("Starting to build index");
    var results = new ArrayList<DocumentPair>();
    try (var sentenceIndex = SentenceIndex.from(embeddings, documentEntries)) {
      seed(documentEntries, sentenceIndex);
//      for (var documentMatchGroup : documentToMatchGroups) {
      for (int i = 0; i < documentToMatchGroups.size(); i++) {
        var matchGroups = documentToMatchGroups.get(i).entrySet();
        var first = documentEntries.get(i);
        for (var matchGroup : matchGroups) {
          var second = documentEntries.get(matchGroup.getKey());
          results.add(createDocumentPair(first, second, matchGroup.getValue()));
        }
//        for (int j = 0; j < matchGroups.size(); j++) {
//          var second = documentEntries.get(j);
//          results.add(createDocumentPair(first, second, matchGroups.get(j)));
//        }
      }

      return results;
    } catch (Exception e) {
      throw new RuntimeException(e);
//      throw new PlagdetException("Failed to create index");
    }
  }

  private void seed(@NotNull List<DocumentEntry> documentEntries, SentenceIndex sentenceIndex) {
    int globalSentenceIndex = 0;
    log.trace("Starting to seed");
    for (int i = 0; i < documentEntries.size(); i++) {
      // clear document hash if present
      var first = documentEntries.get(i);
      log.trace("Seeding document {}/{}", i + 1, documentEntries.size());
//      var vectorsCache = docToSentenceVectors.get(i); // TODO: don't remove it, but memory
//      if (vectorsCache != null) {
//        vectorsCache.clear();
//      }

      var firstSentences = AnnotationUtils.sentences(first.getAnnotation());
      int firstSentencesCount = firstSentences.size();
      var firstSentVectors = new ArrayList<>(
        firstSentences.stream()// TODO: cache can be used here
          .map(embeddings::getWordVectorsWithWeights)
          .toList()
      );

      var documentMatches = new HashMap<Integer, List<int[]>>();
      for (int j = 0; j < firstSentencesCount; j++) {
        List<int[]> matches =
          sentenceIndex.findSimilar(globalSentenceIndex, SENTENCE_VECTOR_SIMILARITY_THRESHOLD);
        globalSentenceIndex++;
        int matchesSize = matches.size();
        for (int k = 0; k < matchesSize; k++) {
          int[] match = matches.get(k);
          if (match[0] == i) {
            // our document
            continue;
          }

          // TODO: such comparison search does not work
          if (match[0] < i && comparisonsDone.contains(String.format("%d%d", match[0], i))) {
            continue;
          }

          var sentenceRepresentation =
            retrieveSentenceRepresentationForMatch(sentenceIndex, match);
          //TODO: validate whether this comparison already exists
          if (isSimilar(firstSentVectors.get(j), sentenceRepresentation)) {
            documentMatches.computeIfAbsent(match[0], (key) -> new ArrayList<>())
              .add(new int[]{j, match[1]});
          }
        }
      }

      var entrySet = documentMatches.entrySet();
      for (var documentMatch : entrySet) {
        comparisonsDone.add(String.format("%d%d", i, documentMatch.getKey()));
      }
      documentToMatchGroups.add(i, documentMatches);
      firstSentVectors.clear();
      System.gc();
    }
  }

  private DocumentPair createDocumentPair(
    DocumentEntry first,
    DocumentEntry second,
    List<int[]> seeds
  ) {
    List<List<CoreMap>> documents = List.of(
      AnnotationUtils.sentences(first.getAnnotation()),
      AnnotationUtils.sentences(second.getAnnotation())
    );
    ExtensionStrategy extensionStrategy = new NopExtensionStrategy(embeddings, 0.7, 1);
    List<Fragment> fragments = extensionStrategy.extend(documents, seeds, 3);
    log.trace("Fragments count: {}", fragments.size());

    DocumentPair result = DocumentPair.builder().first(first).second(second).build();
    double score = 0;
    if (!fragments.isEmpty()) {
      result.setPlagCases(
        fragments.stream()
          .map(f -> f.toMarkingWithType(documents, SpecialMarking.SpecialMarkingType.PLAG))
          .collect(Collectors.toList())
      );

      score = ScoreUtils.plagScoreAsSentenceCoverage(result, fragments);
    }

    result.getPlagScores().add(
      PlagScore.builder().score(score).type(PlagScore.PlagScoreType.SEMANTIC).build()
    );
    return result;
  }

  private @NotNull Pair<INDArray, INDArray> retrieveSentenceRepresentationForMatch(
    @NotNull SentenceIndex sentenceIndex,
    int[] match
  ) {
    var sentence = sentenceIndex.findSentenceByGlobalIndex(match[2]);
    var sentenceCache =
      docToSentenceVectors.computeIfAbsent(match[0], (key) -> new HashMap<>());

    return sentenceCache.computeIfAbsent(
      match[1], (key) -> embeddings.getWordVectorsWithWeights(sentence)
    );
  }

  public boolean isSimilar
    (Pair<INDArray, INDArray> firstSent, Pair<INDArray, INDArray> secondSent) {
    INDArray w1 = firstSent.second();
    INDArray w2 = secondSent.second();

    INDArray cosineMatrix = cosineMatrix(firstSent.first(), secondSent.first());
    if (cosineMatrix == null) return false;

    INDArray max1 = cosineMatrix.max(1);
    INDArray max2 = cosineMatrix.max(0);

    double sim1 = max1.mmul(w1).getDouble(0) / w1.sumNumber().doubleValue();
    double sim2 = max2.mmul(w2).getDouble(0) / w2.sumNumber().doubleValue();

    double sizeRatio = Math.min(
      (double) w1.length() / w2.length(),
      (double) w2.length() / w1.length()
    );

    double similarity = 0.5 * (sim1 + sim2) * sizeRatio;
    return similarity > SIMILARITY_THRESHOLD;
  }

  public static INDArray cosineMatrix(INDArray A, INDArray B) {
    return (A.isEmpty() || B.isEmpty()) ? null : Transforms.allCosineSimilarities(A, B, 1);
  }
}
