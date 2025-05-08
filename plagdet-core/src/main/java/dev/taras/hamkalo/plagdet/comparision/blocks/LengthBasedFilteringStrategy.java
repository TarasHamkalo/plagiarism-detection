package dev.taras.hamkalo.plagdet.comparision.blocks;

import dev.taras.hamkalo.plagdet.comparision.model.Fragment;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LengthBasedFilteringStrategy implements FilteringStrategy {

  @Value
  @Builder
  public static class LengthBasedFilteringStrategyParameters {

    /**
     * The smallest fragment to preserve, length should be met on both documents
     */
    public static final int DEFAULT_MIN_CHARACTER_LENGTH = 90;

    @Builder.Default
    int minCharacterLength = DEFAULT_MIN_CHARACTER_LENGTH;

  }

  @NotNull LengthBasedFilteringStrategyParameters parameters;

  @Override
  public @NotNull List<Fragment> filter(
    @NotNull DocumentPool documentPool,
    @NotNull List<Fragment> fragments
  ) {
    var documents = List.of(
      AnnotationUtils.sentences(documentPool.getSuspiciousDocument().getAnnotation()),
      AnnotationUtils.sentences(documentPool.getSourceDocument().getAnnotation())
    );
    return filter(documents, fragments);
  }

  @Override
  public @NotNull List<Fragment> filter(
    @NotNull List<List<CoreMap>> documents,
    @NotNull List<Fragment> fragments
  ) {
    fragments.removeIf(f -> !this.isLengthMet(documents, f) || isWithingAny(fragments, f));
    return fragments;
  }

  private boolean isWithingAny(
    @NotNull List<Fragment> fragments,
    @NotNull Fragment fragment
  ) {
    boolean hasSusp = fragments.stream().anyMatch(f -> !f.equals(fragment) && fragment.isWithin(f, 0));
    boolean hasSrc = fragments.stream().anyMatch(f -> !f.equals(fragment) && fragment.isWithin(f, 1));
    return hasSusp && hasSrc;
  }

  private boolean isLengthMet(
    @NotNull List<List<CoreMap>> documents,
    @NotNull Fragment fragment
  ) {
    int[] starts = fragment.getSideStarts();
    int[] ends = fragment.getSideEnds();
    int firstLength = AnnotationUtils.end(documents.get(0).get(ends[0])) -
                      AnnotationUtils.begin(documents.get(0).get(starts[0]));
    int secondLength = AnnotationUtils.end(documents.get(1).get(ends[1])) -
                       AnnotationUtils.begin(documents.get(1).get(starts[1]));
    return firstLength >= parameters.minCharacterLength &&
           secondLength >= parameters.minCharacterLength;
  }
}
