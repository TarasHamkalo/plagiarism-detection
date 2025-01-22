package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Seed {

  CoreMap firstSent;

  CoreMap secondSent;

  public int firstOffset() {
    return firstSent.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public int secondOffset() {
    return secondSent.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
  }

  public int getSideOffset(int side) {
    return (side % 2) == 0 ? firstOffset() : secondOffset();
  }

//  public boolean isAdjacent(Seed other, int side, int gap) {
//
//  }

}
