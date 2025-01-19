package dev.taras.hamkalo.plagdet.io;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileData {

  String submitter;

  Path path;

  Metadata metadata;

  @ToString.Exclude
  String content;

}
