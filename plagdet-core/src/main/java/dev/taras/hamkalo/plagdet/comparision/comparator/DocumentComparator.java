package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import org.jetbrains.annotations.NotNull;

public interface DocumentComparator {

  @NotNull ComparisonResult compare(@NotNull DocumentPair pair);

}
