package dev.taras.hamkalo.plagdet;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import lombok.*;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Report {

  int comparisonsCount;

  @NotNull
  @JsonIgnore
  String inputPath;

  @NotNull
  @JsonIgnore
  PlagdetParameters plagdetParameters;

  @JsonGetter(value = "configuration")
  public @NotNull PlagdetParameters getPlagdetParameters() {
    return plagdetParameters;
  }

  @NotNull
  @JsonIgnore
  @Builder.Default
  List<Submission> submissions = new ArrayList<>();

  @NotNull
  @JsonIgnore
  @Builder.Default
  List<DocumentPair> pairs = new ArrayList<>();

  @NotNull
  @JsonIgnore
  @Builder.Default
  List<DocumentEntry> documentsRetrievedFromIndex = new ArrayList<>();

  @With
  @NotNull
  @Builder.Default
  Set<String> comparisonIgnore = new HashSet<>();

  @JsonGetter
  public String getName() {
    return FilenameUtils.getBaseName(inputPath);
  }

  @JsonGetter
  public String getAnalyzedFileName() {
    return FilenameUtils.getName(inputPath);
  }

  @JsonGetter
  public Date getDateAnalyzed() {
    return Date.from(Instant.now());
  }

  public static @Nullable Report from(@NotNull Plagdet plagdet, boolean withIndexCache) {
    if (plagdet.getCurrentInputPath() == null) {
      return null;
    }

    var submissions = plagdet.getSubmissions();
    var deduplicatedComparisonResults = submissions.stream()
      .flatMap(s -> s.getDocuments().stream())
      .flatMap(d -> d.getDocumentPairs().stream())
      .collect(Collectors.toMap(
        (p) -> p.getId(),
        p -> p,
        (p1, p2) -> p1)
      );

    var filteredRetrievedDocuments = SimilarDocumentSource.getRetrievedDocumentsCache().values();
    if (!withIndexCache) {
      SimilarDocumentSource.getRetrievedDocumentsCache().clear();
      filteredRetrievedDocuments = deduplicatedComparisonResults.values().stream()
        .map(DocumentPair::getSecond)
        .filter(DocumentEntry::isIndexed)
        .toList();
    }

    return Report.builder()
      .inputPath(plagdet.getCurrentInputPath())
      .plagdetParameters(plagdet.getParameters())
      .submissions(submissions)
      .comparisonsCount(plagdet.getComparisonPipeline().getComparisonsCount())
      .pairs(new ArrayList<>(deduplicatedComparisonResults.values()))
      .documentsRetrievedFromIndex(new ArrayList<>(filteredRetrievedDocuments))
      .build();
  }
}
