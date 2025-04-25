package dev.taras.hamkalo.plagdet.io.report.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.taras.hamkalo.plagdet.PlagdetParameters;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

@Value
@Builder
public class OverviewJsonModel {

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

}
