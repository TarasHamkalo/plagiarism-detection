package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.comparision.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.comparision.blocks.ExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.blocks.NopExtensionStrategy;
import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.comparision.util.ScoreUtils;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.taras.hamkalo.plagdet.comparision.SpecialMarking.SpecialMarkingType;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndexBasedSentenceComparator {

  @NotNull Embeddings embeddings;

  Map<Integer, HashMap<Integer, List<int[]>>> documentToMatchGroups;
  Map<Integer, DocumentEntry> idToDocument;

  public static final float SENTENCE_VECTOR_SIMILARITY_THRESHOLD = 0.7f;
  public static final float SIMILARITY_THRESHOLD = 0.65f;


  Map<Integer, Map<Integer, Pair<INDArray, INDArray>>> docToSentenceVecs1 = new HashMap<>();
  Map<Integer, Map<Integer, Pair<INDArray, INDArray>>> docToSentenceVecs2 = new HashMap<>();

  public List<DocumentPair> compare(@NotNull List<DocumentEntry> documentEntries) {
    documentToMatchGroups = new HashMap<>();
    idToDocument = new HashMap<>(documentEntries.size());
    documentEntries.forEach(d -> idToDocument.put(d.getId(), d));

    SentenceIndex sentenceIndex = new SentenceIndex(embeddings);
    log.trace("Starting to build index");
    sentenceIndex.buildFromDocuments(documentEntries);

    for (int i = 0; i < documentEntries.size(); i++) {
      List<CoreMap> sentences = AnnotationUtils.sentences(documentEntries.get(i).getAnnotation());
      int sentenceCount = sentences.size();

      var documentMatches = new HashMap<Integer, List<int[]>>();
      for (int j = 0; j < sentenceCount; j++) {
        List<int[]> matches = sentenceIndex.findSimilar(sentences.get(j), SENTENCE_VECTOR_SIMILARITY_THRESHOLD);

        for (int[] match : matches) {
          if (match[0] <= i) {
            continue;
          }

          documentMatches
            .computeIfAbsent(match[0], key -> new ArrayList<>())
            .add(new int[]{j, match[1]});
        }
      }
      documentToMatchGroups.put(documentEntries.get(i).getId(), documentMatches);
    }

    log.trace("Ended initial seeding");
    return compareUsingIndex(documentEntries);
  }

  private List<DocumentPair> compareUsingIndex(@NotNull List<DocumentEntry> documentEntries) {
    ExtensionStrategy extensionStrategy = new NopExtensionStrategy(embeddings, 0.7, 1);
    var results = new ArrayList<DocumentPair>();

    for (DocumentEntry first : documentEntries) {
      List<CoreMap> firstSentences = AnnotationUtils.sentences(first.getAnnotation());
      int firstSentencesSize = firstSentences.size();

      var matchGroups = documentToMatchGroups.get(first.getId());
      if (matchGroups == null) continue;

      for (var matchGroup : matchGroups.entrySet()) {
        DocumentEntry second = idToDocument.get(matchGroup.getKey());
        List<CoreMap> secondSentences = AnnotationUtils.sentences(second.getAnnotation());
        int secondSentencesSize = secondSentences.size();

        if (matchGroup.getValue().size() < (firstSentencesSize * secondSentencesSize * 0.1))
          continue;

        results.add(
          createDocumentPair(matchGroup, firstSentences, secondSentences, extensionStrategy, first, second)
        );
      }
    }

    return results;
  }

  private DocumentPair createDocumentPair(
    Map.Entry<Integer, List<int[]>> matchGroup,
    List<CoreMap> firstSentences,
    List<CoreMap> secondSentences,
    ExtensionStrategy extensionStrategy,
    DocumentEntry first,
    DocumentEntry second
  ) {
    List<List<CoreMap>> documents = List.of(firstSentences, secondSentences);
    List<int[]> seeds = seedFromInitial(first.getId(), second.getId(), documents, matchGroup.getValue());

    List<Fragment> fragments = extensionStrategy.extend(documents, seeds, 3);
    log.trace("Fragments count: {}", fragments.size());

    DocumentPair result = DocumentPair.builder()
      .first(first)
      .second(second)
      .build();

    double score = 0;
    if (!fragments.isEmpty()) {
      result.setPlagCases(
        fragments.stream()
          .map(f -> f.toMarkingWithType(documents, SpecialMarkingType.PLAG))
          .collect(Collectors.toList())
      );

      score = ScoreUtils.plagScoreAsSentenceCoverage(result, fragments);

    }

    result.getPlagScores().add(
      PlagScore.builder()
        .score(score)
        .type(PlagScore.PlagScoreType.SEMANTIC)
        .build()
    );
    return result;
  }

  public List<int[]> seedFromInitial(
    int firstDocId,
    int secondDocId,
    List<List<CoreMap>> documentsSentences,
    List<int[]> initialSeeds
  ) {
    if (initialSeeds.isEmpty()) return List.of();

    Map<Integer, Pair<INDArray, INDArray>> cache1 =
      docToSentenceVecs1.computeIfAbsent(firstDocId, k -> new HashMap<>());
    Map<Integer, Pair<INDArray, INDArray>> cache2 =
      docToSentenceVecs2.computeIfAbsent(secondDocId, k -> new HashMap<>());

    List<int[]> filteredSeeds = new ArrayList<>();
    List<CoreMap> sents1 = documentsSentences.get(0);
    List<CoreMap> sents2 = documentsSentences.get(1);

    for (int[] seed : initialSeeds) {
      int idx1 = seed[0], idx2 = seed[1];

      Pair<INDArray, INDArray> w1 = cache1.computeIfAbsent(idx1, i ->
        embeddings.getWordVectorsWithWeights(sents1.get(i)));
      Pair<INDArray, INDArray> w2 = cache2.computeIfAbsent(idx2, i ->
        embeddings.getWordVectorsWithWeights(sents2.get(i)));

      if (isSimilar(w1, w2)) {
        filteredSeeds.add(seed);
      }
    }

    return filteredSeeds;
  }

  public boolean isSimilar(Pair<INDArray, INDArray> firstSent, Pair<INDArray, INDArray> secondSent) {
    INDArray w1 = firstSent.second().reshape(firstSent.second().length(), 1);
    INDArray w2 = secondSent.second().reshape(secondSent.second().length(), 1);

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
