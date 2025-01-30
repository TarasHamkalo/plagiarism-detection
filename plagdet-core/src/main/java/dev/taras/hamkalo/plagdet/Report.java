package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class Report {

  int comparisonsCount;

  @NotNull
  @Builder.Default
  List<Submission> submissions = new ArrayList<>();

  @NotNull
  @Builder.Default
  List<DocumentPair> pairs = new ArrayList<>();

  @NotNull
  @Builder.Default
  List<DocumentEntry> documentsRetrievedFromIndex = new ArrayList<>();

}
