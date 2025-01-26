package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Direction;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.util.Sets;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShingleComparator implements DocumentComparator {

  int size;

  int overlap;

  double plagiarizedLevel;

  double discardLevel;

  boolean bypassFiltering;

  Map<Integer, Set<Shingle>> documentToShingle;

  public ShingleComparator(double plagiarizedLevel,
                           double discardLevel,
                           int size,
                           int overlap,
                           boolean bypassFiltering) {
    if (overlap > size - 1) {
      throw new IllegalArgumentException("Overlap should be less than size");
    }

    this.size = size;
    this.overlap = overlap;
    this.plagiarizedLevel = plagiarizedLevel;
    this.discardLevel = discardLevel;
    this.documentToShingle = new HashMap<>();
    this.bypassFiltering = bypassFiltering;
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    if (documentPair.getFirst().isTemplate() || documentPair.getSecond().isTemplate()) {
      return ComparisonResult.CONTINUE;
    }

    var firstShingles = getDocumentShingles(documentPair.getFirst());
    var secondShingles = getDocumentShingles(documentPair.getSecond());
    double score = jaccardSimilarity(firstShingles, secondShingles);
    ComparisonResult result = ComparisonResult.CONTINUE;
    List<String> comments = new ArrayList<>();
    if(score > plagiarizedLevel) {
      result = ComparisonResult.PLAGIARIZED;
      comments.add(
        "[PLAGIARIZED] Jaccard similarity %s greater than %s".formatted(score, plagiarizedLevel)
      );
    } else if (score < discardLevel) {
      result = ComparisonResult.DISCARD;
      comments.add("[DISCARD] Jaccard similarity: %s lass than %s".formatted(score, discardLevel));
    }

    documentPair.getPlagScores().add(
      PlagScore.builder()
        .score(score)
        .type(PlagScore.PlagScoreType.JACCARD)
        .comments(comments)
        .build()
    );

    return bypassFiltering ? ComparisonResult.CONTINUE : result;
  }

  private Set<Shingle> getDocumentShingles(@NotNull DocumentEntry documentEntry) {
    return documentToShingle.computeIfAbsent(
      documentEntry.getId(), (key) -> generateShingles(documentEntry.getCoreDocument())
    );
  }

  private Set<Shingle> generateShingles(@NotNull CoreDocument coreDocument) {
    final int step = size - overlap;

    var wordTokens = coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(t ->
        !t.get(PlagdetAnnotations.StopWordAnnotation.class) ||
        !t.get(PlagdetAnnotations.CodeSegmentAnnotation.class)
      ).toList();


    var shingleSet = new HashSet<Shingle>();
    for (int i = 0; i <= wordTokens.size() - size; i += step) {
      shingleSet.add(Shingle.from(wordTokens.subList(i, i + size)));
    }

    return shingleSet;
  }

  public static double jaccardSimilarity(Set<Shingle> first, Set<Shingle> second) {
    double unionSize = Sets.union(first, second).size() * 1.0;
    double intersectionSize = Sets.intersection(first, second).size() * 1.0;

    return intersectionSize / unionSize;
  }

  @Data
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Shingle {

    @NotNull
    String content;

    @NotNull
    @EqualsAndHashCode.Exclude
    Integer start;

    @NotNull
    @EqualsAndHashCode.Exclude
    Integer end;

    public static Shingle from(List<CoreLabel> tokens) {
      return Shingle.builder()
        .start(tokens.get(0).beginPosition())
        .end(tokens.get(tokens.size() - 1).endPosition())
        .content(tokensToLemmaString(tokens))
        .build();
    }

    private static @NotNull String tokensToLemmaString(List<CoreLabel> tokens) {
      return tokens.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
    }

  }

}
