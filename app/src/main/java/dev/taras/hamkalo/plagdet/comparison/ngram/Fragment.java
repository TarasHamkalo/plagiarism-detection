package dev.taras.hamkalo.plagdet.comparison.ngram;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import javax.swing.text.Document;
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
