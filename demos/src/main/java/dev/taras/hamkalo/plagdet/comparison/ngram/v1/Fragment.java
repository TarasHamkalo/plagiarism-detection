package dev.taras.hamkalo.plagdet.comparison.ngram.v1;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
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
public class Fragment {

  @ToString.Exclude
  DocumentEntry documentEntry;

  List<Ngram> containedNgram;

  Integer start;

  Integer end;

}
