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

  Map<Integer, Set<Shingle>> documentToShingle;

  int size;

  int overlap;

  double plagiarizedLevel;

  double discardLevel;

  public ShingleComparator(double plagiarizedLevel, double discardLevel, int size, int overlap) {
    if (overlap > size - 1) {
      throw new IllegalArgumentException("Overlap should be less than size");
    }

    this.size = size;
    this.overlap = overlap;
    this.plagiarizedLevel = plagiarizedLevel;
    this.discardLevel = discardLevel;
    this.documentToShingle = new HashMap<>();
  }

  public @NotNull ComparisonResult compare(@NotNull DocumentPair documentPair) {
    if (documentPair.getFirst().isTemplate() || documentPair.getSecond().isTemplate()) {
      return ComparisonResult.CONTINUE;
    }

    var firstShingles = getDocumentShingles(documentPair.getFirst());
    var secondShingles = getDocumentShingles(documentPair.getSecond());
    var plagScore = PlagScore.builder()
      .score(jaccardSimilarity(firstShingles, secondShingles))
      .direction(Direction.UNCERTAIN)
      .comments(List.of("Jaccard coefficient"))
      .build();

    documentPair.getPlagScores().add(plagScore);

    if(plagScore.getScore() > plagiarizedLevel) {
      return ComparisonResult.PLAGIARIZED;
    } else if (plagScore.getScore() < discardLevel) {
      return ComparisonResult.DISCARD;
    }

    return ComparisonResult.CONTINUE;
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
