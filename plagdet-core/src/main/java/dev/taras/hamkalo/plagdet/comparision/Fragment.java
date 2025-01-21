package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.pipeline.CoreSentence;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fragment {

  Set<CoreSentence> firstSentences;

  Set<CoreSentence> secondSentences;
}
