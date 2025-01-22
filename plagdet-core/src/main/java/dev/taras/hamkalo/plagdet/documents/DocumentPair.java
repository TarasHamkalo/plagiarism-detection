package dev.taras.hamkalo.plagdet.documents;


import dev.taras.hamkalo.plagdet.comparision.PlagCase;
import dev.taras.hamkalo.plagdet.comparision.PlagScore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentPair {

  @NotNull DocumentEntry first;

  @NotNull DocumentEntry second;

  @NotNull
  @Builder.Default
  List<PlagCase> plagCases = new ArrayList<>();

  @NotNull
  @Builder.Default
  Set<PlagScore> plagScores = new HashSet<>();

}