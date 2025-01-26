package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator.SentenceComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator.ShingleComparatorParameters;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
@Builder
public class PlagdetParameters {

  @Nullable String luceneIndexDirectoryPath;

  @NotNull SentenceComparatorParameters sentenceComparatorParameters;

  @NotNull ShingleComparatorParameters shingleComparatorParameters;

  @NotNull MetadataComparatorParameters metadataComparatorParameters;

}
