package dev.taras.hamkalo.plagdet.comparision;

import dev.taras.hamkalo.plagdet.comparision.comparator.ComparisonResult;
import dev.taras.hamkalo.plagdet.comparision.comparator.DocumentComparator;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComparisonPipeline {

  List<DocumentComparator> comparators;

  public ComparisonPipeline(@NotNull DocumentComparator... documentComparators) {
    this.comparators = new ArrayList<>(Arrays.asList(documentComparators));
  }

  public ComparisonPipeline addComparator(DocumentComparator comparator) {
    this.comparators.add(comparator);
    return this;
  }

  public void compare(List<DocumentPair> pairs) {
    for (DocumentPair pair : pairs) {
      for (DocumentComparator comparator : comparators) {
        ComparisonResult result = comparator.compare(pair);
        if (!ComparisonResult.CONTINUE.equals(result)) {
          break;
        }
      }
    }
  }
}
