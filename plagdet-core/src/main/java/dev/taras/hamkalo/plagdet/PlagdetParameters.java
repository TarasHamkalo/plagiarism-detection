package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.MetadataComparator.MetadataComparatorParameters;
import dev.taras.hamkalo.plagdet.comparision.filter.ShingleOverlapComparisonFilter.ShingleOverlapComparisonFilterParameters;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.taras.hamkalo.plagdet.ComparisonsStore.*;
import static dev.taras.hamkalo.plagdet.comparision.filter.LengthComparisonFilter.LengthComparisonFilterParameters;


@Value
@Builder
public class PlagdetParameters {

  @Nullable String luceneIndexDirectoryPath;

  @Nullable String templateDocumentPath;

  @NotNull Integer numberOfProcessors;

  @NotNull MetadataComparatorParameters metadataComparatorParameters;

  @NotNull ShingleOverlapComparisonFilterParameters shingleOverlapComparisonFilterParameters;

  @NotNull LengthComparisonFilterParameters lengthComparisonFilterParameters;

  @NotNull ComparisonsStoreParameters comparisonsStoreParameters;

}
