package dev.taras.hamkalo.plagdet.comparision;

import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fragment {

  List<Seed> seeds;

}
