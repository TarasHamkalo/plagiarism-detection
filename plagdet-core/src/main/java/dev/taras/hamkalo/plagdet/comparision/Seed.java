package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Seed {

  int firstSentIndex;

  int secondSentIndex;

  CoreSentence firstSent;

  CoreSentence secondSent;

  public int firstOffset() {
    return firstSent.coreMap().get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public int secondOffset() {
    return secondSent.coreMap().get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public int getSideOffset(int side) {
    return (side % 2) == 0 ? firstOffset() : secondOffset();
  }

  public boolean isAdjacent(Seed other, int side, int gap) {
    if (side % 2 == 0) {
      return Math.abs(firstSentIndex - other.firstSentIndex) < gap;
    } else {
      return Math.abs(secondSentIndex - other.secondSentIndex) < gap;
    }
  }

}
