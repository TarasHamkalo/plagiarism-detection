package dev.taras.hamkalo.plagdet.preprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.taras.hamkalo.plagdet.preprocessing.cleaner.LinkRemover;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class LinkRemoverTest {

  String text = """
      link secure https://blabla.com/12344
      link not secure http://blabla.com/va/
      @discord tags remove 
      blab www.doi.org/ www.doi.org https://doi.org/10.1080/10509585.2015.1092083
      linux.die.net
      doi.org
      man7.org
      struct.struct.var 
      7. Použitá literatúra a informačné zdroje
      2016 - Sofia - Pipes [2] 
      2016 - Sofia - Signaly
      2016 - Sofia - Zdieľaná pamäť
      2016 - Sofia - Semafory
      2016 - Sofia - Sockety (Networking)
      https://beej.us/guide/bgipc/html/split/
      https://man7.org/linux/man-pages/
      https://linux.die.net/
      https://stackoverflow.com/
      """;

  LinkRemover linkRemover = new LinkRemover();

  @Test
  void testRemoveLinks() {
    final var expected = """
      link secure
      link not secure 
       tags remove 
      blab
      
      
      
      struct.struct.var 
      7. Použitá literatúra a informačné zdroje
      2016 - Sofia - Pipes
      2016 - Sofia - Signaly
      2016 - Sofia - Zdieľaná pamäť
      2016 - Sofia - Semafory
      2016 - Sofia - Sockety (Networking)
      
      
      
      
      """;

    String result = linkRemover.process(text);
    assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result));
  }

  // Helper method to normalize whitespace
  private String normalizeWhitespace(String input) {
    return input.replaceAll("\\s+", " ").trim();
  }
}
