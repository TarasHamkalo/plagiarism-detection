package dev.taras.hamkalo.plagdet.comparision.filter;

import dev.taras.hamkalo.plagdet.comparision.model.FilteringResult;
import dev.taras.hamkalo.plagdet.documents.pool.DocumentPool;
import org.jetbrains.annotations.NotNull;

public interface ComparisonFilter {

  @NotNull FilteringResult doFilter(@NotNull DocumentPool documentPool);

}
