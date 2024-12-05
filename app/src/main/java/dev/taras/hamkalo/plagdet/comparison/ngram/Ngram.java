package dev.taras.hamkalo.plagdet.comparison.ngram;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Objects;

@Value
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ngram {

  List<String> contentStrings;

  Integer start;

  Integer end;

//  @Override
//  public boolean equals(Object o) {
//    if (this == o) return true;
//    if (o == null || getClass() != o.getClass()) return false;
//    Ngram other = (Ngram) o;
//    // compare ordered
//    return Objects.equals(other.contentStrings, contentStrings);
//  }
//
//  @Override
//  public int hashCode() {
//    return Objects.hash(contentStrings);
//  }
//
}
