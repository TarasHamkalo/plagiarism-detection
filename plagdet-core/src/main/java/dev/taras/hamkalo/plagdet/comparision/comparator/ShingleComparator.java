package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.util.Sets;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShingleComparator implements DocumentComparator {

  @Value
  @Builder
  @AllArgsConstructor
  public class ShingleComparatorParameters {

    public static final int DEFAULT_SIZE = 2;

    public static final int DEFAULT_OVERLAP = 1;

    public static final double DEFAULT_PLAGIARIZED_LEVEL = 0.8;

    public static final double DEFAULT_DISCARD_LEVEL = 0.1;

    public static final boolean DEFAULT_BYPASS_FILTERING = false;

    @Builder.Default
    int size = DEFAULT_SIZE;

    @Builder.Default
    int overlap = DEFAULT_OVERLAP;

    @Builder.Default
    double plagiarizedLevel = DEFAULT_PLAGIARIZED_LEVEL;

    @Builder.Default
    double discardLevel = DEFAULT_DISCARD_LEVEL;

    @Builder.Default
    boolean bypassFiltering = DEFAULT_BYPASS_FILTERING;

  }

  ShingleComparatorParameters parameters;

  Map<Integer, Set<Shingle>> documentToShingle;

  public ShingleComparator(ShingleComparatorParameters parameters) {
    if (parameters.overlap > parameters.size - 1) {
      throw new IllegalArgumentException("Overlap should be less than size");
    }

    this.parameters = parameters;
    this.documentToShingle = new HashMap<>();
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    var firstShingles = getDocumentShingles(documentPair.getFirst());
    var secondShingles = getDocumentShingles(documentPair.getSecond());
    double score = jaccardSimilarity(firstShingles, secondShingles);
    ComparisonResult result = ComparisonResult.CONTINUE;
    List<String> comments = new ArrayList<>();
    if(score > parameters.plagiarizedLevel) {
      result = ComparisonResult.PLAGIARIZED;
      comments.add(
        "[PLAGIARIZED] Jaccard similarity %s greater than %s".formatted(score, parameters.plagiarizedLevel)
      );
    } else if (score < parameters.discardLevel) {
      result = ComparisonResult.DISCARD;
      comments.add("[DISCARD] Jaccard similarity: %s lass than %s".formatted(score, parameters.discardLevel));
    }

    documentPair.getPlagScores().add(
      PlagScore.builder()
        .score(score)
        .type(PlagScore.PlagScoreType.JACCARD)
        .comments(comments)
        .build()
    );

    return parameters.bypassFiltering ? ComparisonResult.CONTINUE : result;
  }

  private Set<Shingle> getDocumentShingles(@NotNull DocumentEntry documentEntry) {
    return documentToShingle.computeIfAbsent(
      documentEntry.getId(), (key) -> generateShingles(documentEntry.getCoreDocument())
    );
  }

  private Set<Shingle> generateShingles(@NotNull CoreDocument coreDocument) {
    final int step = parameters.size - parameters.overlap;

    var wordTokens = coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(t ->
        !t.get(PlagdetAnnotations.StopWordAnnotation.class) ||
        !t.get(PlagdetAnnotations.CodeSegmentAnnotation.class)
      ).toList();


    var shingleSet = new HashSet<Shingle>();
    for (int i = 0; i <= wordTokens.size() - parameters.size; i += step) {
      shingleSet.add(Shingle.from(wordTokens.subList(i, i + parameters.size)));
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
