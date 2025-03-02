package dev.taras.hamkalo.plagdet.io;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

  @JsonGetter
  public String getFilename() {
    return FilenameUtils.getBaseName(persistentPath);
  }

  @JsonGetter
  public String getExtension() {
    return FilenameUtils.getExtension(persistentPath);
  }

}
