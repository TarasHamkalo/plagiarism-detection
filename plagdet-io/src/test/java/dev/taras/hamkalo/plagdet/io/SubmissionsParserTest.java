package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.io.reader.TextReader;
import dev.taras.hamkalo.plagdet.io.reader.WordDocumentReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SubmissionsParserTest {

  @Test
  void whenZippedSubmissionsDoNotContainZipFilesShouldNotThrow() {
    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/zipped.zip";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new WordDocumentReader(), new TextReader()),
      List.of(new WordDocumentReader())
    );

    assertDoesNotThrow(() -> submissionsParser.parseSubmissions(zipPath));
  }

  @Test
  void whenZippedSubmissionsContainZipFilesShouldReturnThem() {
    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/2019_2020_zipped.zip";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new WordDocumentReader(), new TextReader()),
      List.of(new WordDocumentReader())
    );

    final var submissions = submissionsParser.parseSubmissions(zipPath);
    for (var submission : submissions) {
      System.out.println(submission);
    }
    assertThat(!submissions.isEmpty());
  }
}