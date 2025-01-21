package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fragment {

  Set<CoreMap> firstSentences;

  Set<CoreMap> secondSentences;

}
