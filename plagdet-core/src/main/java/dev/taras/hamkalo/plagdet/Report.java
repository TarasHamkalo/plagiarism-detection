package dev.taras.hamkalo.plagdet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.DocumentPair;
import dev.taras.hamkalo.plagdet.documents.Submission;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

  public String getName() {
    return FilenameUtils.getBaseName(inputPath);
  }

  public String getAnalyzedFileName() {
    return FilenameUtils.getName(inputPath);
  }

  public Date getDateAnalyzed() {
    return Date.from(Instant.now());
  }

}
