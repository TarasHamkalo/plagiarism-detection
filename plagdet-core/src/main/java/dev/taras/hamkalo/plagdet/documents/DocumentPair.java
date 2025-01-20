package dev.taras.hamkalo.plagdet.documents;


import dev.taras.hamkalo.plagdet.comparision.Fragment;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;


@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentPair {

  DocumentEntry first;

  DocumentEntry second;

  Set<Fragment> commonFragments;

}