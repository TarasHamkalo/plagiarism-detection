package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.comparision.Direction;
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

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShingleComparator {

  Map<String, Set<Shingle>> documentToShingle;

  int size;

  int overlap;

  public ShingleComparator(int size, int overlap) {
    if (overlap >= size - 1) {
      throw new IllegalArgumentException("Overlap should be less than size");
    }

    this.size = size;
    this.overlap = overlap;
  }

  // TODO: add mark for template submission
  public void compare(DocumentPair documentPair) {
    if (documentPair.getFirst().isTemplate() || documentPair.getSecond().isTemplate()) {
      return;
    }

    var firstShingles = getDocumentShingles(documentPair.getFirst());
    var secondShingles = getDocumentShingles(documentPair.getSecond());
    documentPair.getPlagScores().add(
      PlagScore.builder()
        .score(jaccardSimilarity(firstShingles, secondShingles))
        .direction(Direction.UNCERTAIN)
        .comments(List.of("Jaccard coefficient"))
        .build()
    );

  }

  private Set<Shingle> getDocumentShingles(@NotNull DocumentEntry documentEntry) {
    return documentToShingle.computeIfAbsent(
      documentEntry.getId(), (key) -> generateShingles(documentEntry.getCoreDocument())
    );
  }

  private Set<Shingle> generateShingles(CoreDocument coreDocument) {
    final int step = size - overlap;

    var wordTokens = coreDocument.annotation().get(PlagdetAnnotations.WordsAnnotation.class).stream()
      .filter(t ->
        t.get(PlagdetAnnotations.StopWordAnnotation.class) &&
        t.get(PlagdetAnnotations.CodeSegmentAnnotation.class)
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
