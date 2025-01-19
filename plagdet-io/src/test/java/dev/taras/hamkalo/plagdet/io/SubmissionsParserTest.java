package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.io.reader.DocDocumentReader;
import dev.taras.hamkalo.plagdet.io.reader.TextReader;
import dev.taras.hamkalo.plagdet.io.reader.DocxDocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SubmissionsParserTest {

  @Test
  void whenZippedSubmissionsDoNotContainZipFilesShouldNotThrow() {
    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/zipped.zip";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new DocxDocumentReader(), new TextReader()),
      List.of(new DocxDocumentReader())
    );

    assertDoesNotThrow(() -> submissionsParser.parseSubmissions(zipPath));
  }

  @Test
  void whenZippedSubmissionsContainZipFilesShouldReturnThem() {
    final String zipPath = "/home/taras-hamkalo/repositories/metadata-pages/19-20/2019_2020_zipped.zip";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new DocxDocumentReader(), new TextReader()),
      List.of(new DocxDocumentReader())
    );

    final var submissions = submissionsParser.parseSubmissions(zipPath);
    for (var submission : submissions) {
      System.out.println(submission);
    }
    assertThat(!submissions.isEmpty());
  }

  @Test
  void whenFlatDirectoryShouldReturnThemAsSeparateSubmissions() {
    final String zipPath = "/home/taras-hamkalo/other/ipc/flatten/16-17/docx";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new DocxDocumentReader(), new TextReader()),
      List.of(new DocxDocumentReader())
    );

    final var submissions = submissionsParser.parseSubmissions(zipPath);
    for (var submission : submissions) {
      System.out.println(submission);
    }
    assertThat(!submissions.isEmpty());
  }

  @Test
  void whenDirectoryContainingZippedFilesShouldReturnThem() {
    final String zipPath = "/home/taras-hamkalo/other/ipc/full/21-22";
    SubmissionsParser submissionsParser = new SubmissionsParser(
      List.of(new DocxDocumentReader(), new DocDocumentReader(), new TextReader()),
      List.of(new DocxDocumentReader(), new DocDocumentReader())
    );

    final var submissions = submissionsParser.parseSubmissions(zipPath);
    for (var submission : submissions) {
      System.out.println(submission);
    }
    assertThat(!submissions.isEmpty());
  }
}