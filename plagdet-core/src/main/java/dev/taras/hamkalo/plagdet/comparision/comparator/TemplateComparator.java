package dev.taras.hamkalo.plagdet.comparision.comparator;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import org.jetbrains.annotations.NotNull;

public interface TemplateComparator {

  /**
   * Mutates @target setting special markings to overlap of target and template content
   * @param target document to compare
   * @param template document representing template
   */
  void compareWithTemplate(@NotNull DocumentEntry target, @NotNull DocumentEntry template);

}
