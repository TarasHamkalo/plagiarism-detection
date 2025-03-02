package dev.taras.hamkalo.plagdet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import dev.taras.hamkalo.plagdet.indexing.SimilarDocumentSource;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder
public class Report {

  int comparisonsCount;

  @NotNull
  @JsonIgnore
  String inputPath;

  @NotNull
  @JsonProperty("configuration")
  PlagdetParameters plagdetParameters;

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

  public String getName() {
    return FilenameUtils.getBaseName(inputPath);
  }

  public String getAnalyzedFileName() {
    return FilenameUtils.getName(inputPath);
  }

  public Date getDateAnalyzed() {
    return Date.from(Instant.now());
  }

  public static @Nullable Report from(@NotNull Plagdet plagdet) {
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

    SimilarDocumentSource.getRetrievedDocumentsCache().clear();
    var filteredRetrievedDocuments = deduplicatedComparisonResults.values().stream()
      .map(DocumentPair::getSecond)
      .filter(DocumentEntry::isIndexed)
      .toList();

    return Report.builder()
      .inputPath(plagdet.getCurrentInputPath())
      .plagdetParameters(plagdet.getParameters())
      .submissions(submissions)
      .comparisonsCount(plagdet.getComparisonPipeline().getComparisonsCount())
      .pairs(new ArrayList<>(deduplicatedComparisonResults.values()))
      .documentsRetrievedFromIndex(filteredRetrievedDocuments)
      .build();
  }
}
