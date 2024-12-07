package dev.taras.hamkalo.plagdet.comparison.ngram;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ngram {

  List<String> contentStrings;

  Integer start;

  Integer end;

}
