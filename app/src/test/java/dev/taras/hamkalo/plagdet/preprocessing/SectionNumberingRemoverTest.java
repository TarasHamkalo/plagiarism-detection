package dev.taras.hamkalo.plagdet.preprocessing;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
class SectionNumberingRemoverTest {

  SectionNumberingRemover remover = new SectionNumberingRemover();

  @Test
  void testRemoveOuterSectionNumbering() {
    String input = """
    1. Section Title
    2. Another Section
    2.1 Subsection
    """;

    String expected = " Section Title\n Another Section\n Subsection\n";
    String actual = remover.process(input);
    assertEquals(expected, actual);
  }

  @Test
  void testRemoveInnerSectionNumbering() {
    String input = "This is a text with a section reference 1.1.1 and another one 2.3.4.5.6";
    String expected = "This is a text with a section reference  and another one ";
    String actual = remover.process(input);
    assertEquals(expected, actual);
  }

  @Test
  void testCombinedRemoval() {
    String input = "1 Section Title 1.1.1 with inner reference and a simple number 2 2.2 \n2. Another Section 2.2.2";
    String expected = " Section Title  with inner reference and a simple number 2 2.2 \n Another Section \n";
    String actual = remover.process(input);
    assertEquals(expected, actual);
  }

  @Test
  void testNoRemovalForNonSectionNumbering() {
    String input = "This is a text without any section numbering.";
    String expected = input;
    String actual = remover.process(input);
    assertEquals(expected, actual);
  }

  @Test
  void testHandlesEmptyInput() {
    String input = "";
    String expected = "";
    String actual = remover.process(input);
    assertEquals(expected, actual);
  }

}