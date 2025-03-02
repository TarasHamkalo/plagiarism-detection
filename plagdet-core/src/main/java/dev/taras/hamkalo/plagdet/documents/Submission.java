package dev.taras.hamkalo.plagdet.documents;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.List;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Submission {

  Path path;

  List<DocumentEntry> documents;

}
