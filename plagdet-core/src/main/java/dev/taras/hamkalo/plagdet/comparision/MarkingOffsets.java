package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MarkingOffsets implements Comparable<MarkingOffsets> {

  int start;

  int end;

  int length;

  public static MarkingOffsets from(@NotNull CoreMap sentence) {
    int start = sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
    int end = sentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
    return new MarkingOffsets(start, end, end - start);
  }

  public boolean overlaps(MarkingOffsets other) {
    return (other.start >= this.start && other.start <= this.end) ||
           (this.start >= other.start && this.start <= other.end);
  }

  @Override
  public int compareTo(@NotNull MarkingOffsets o) {
    if (this.start - o.start != 0) {
      return this.start - o.start;
    }

    return this.end - o.end;
  }
}
