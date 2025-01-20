package dev.taras.hamkalo.plagdet.documents;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Submission {

  Path path;

  List<DocumentEntry> documents;

}
