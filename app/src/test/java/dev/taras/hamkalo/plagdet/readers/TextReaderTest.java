package dev.taras.hamkalo.plagdet.readers;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TextReaderTest {
  public static final String TEST_PATH = "src/test/resources/test.txt";

  @Test
  public void whenGetContentCalledWholeFileShouldBeRead() {
    final var testFilePath = Path.of(TEST_PATH);
    final var expected = """
    This is the first line
    This is the second line""";

    var reader = new TextReader();
    var content = reader.getContent(testFilePath);

    assertEquals(expected, content);
  }

  @Test
  void whenNoFileExistShouldThrowContentReadException() {
    final var invalidPath = Path.of("non-existent-file.txt");

    var reader = new TextReader();

    assertThrows(ContentReadException.class, () -> reader.getContent(invalidPath));
  }
}