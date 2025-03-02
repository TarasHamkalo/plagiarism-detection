package dev.taras.hamkalo.plagdet.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileData {

  String submitter;

  Path temporaryPath;

  // used to identify documents, when zipped the temp path will be like
  // /tmp/extracted.../ARCHIVE_NAME, this will be entry name if so
  String persistentPath;

  Metadata metadata;

  @JsonIgnore
  @ToString.Exclude
  String content;

  public String getFilename() {
    return FilenameUtils.getBaseName(persistentPath);
  }

  public String getExtension() {
    return FilenameUtils.getExtension(persistentPath);
  }

}
