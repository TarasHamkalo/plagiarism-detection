package dev.taras.hamkalo.plagdet.preprocessing;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

// TODO: move this phase after tokenization
// TODO: also removes IPs
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionNumberingRemover implements Preprocessor {


  boolean clearInlineSections;

  boolean clearOuterSections;

  /*
    (1) section
    (2.) section
    (2.1) subsection
    (2.1.1) subsection
    (2.1.1.) subsection
  */
  public static final Pattern SECTION_NUMBERING_PATTERN = Pattern.compile(
    "^\\s*(\\d{1,3}\\.?(?:\\d{1,4}\\.?)*)?"
  );

  /*
    Section references are problematic as it could be the number (1.1)
    But this pattern will try to remove all refs with more than 1 dot (1.1.)
   */
  public static final Pattern INLINE_SECTION_NUMBERING = Pattern.compile(
    "(\\d{1,3}\\.){2}(:?\\d\\.?){0,10}"
  );

  @Override
  public String getName() {
    return "Section numbering remover";
  }

  @Override
  public String process(@NonNull String text) {
    var stringBuilder = new StringBuilder();
    for (var line : text.split("\n")) {
      var cleanOuter = removeOuterSectionNumbering(line);
      var clean = removeInnerSectionNumbering(cleanOuter);
      if (text.isBlank()) {
        // TODO: or leave it
        continue;
      }

      stringBuilder.append(clean).append("\n");
    }

    return stringBuilder.toString();
  }

  private String removeInnerSectionNumbering(String text) {
    if (!clearInlineSections) {
      return text;
    }

    var matcher = INLINE_SECTION_NUMBERING.matcher(text);
    return matcher.replaceAll((result) -> {
      log.trace(
        "Removing section number '{}'",
        text.substring(result.start(), result.end())
      );
      return "";
    });
  }

  private String removeOuterSectionNumbering(String text) {
    if (!clearOuterSections) {
      return text;
    }

    var matcher = SECTION_NUMBERING_PATTERN.matcher(text);
    return matcher.replaceAll((result) -> {
      log.trace(
        "Removing section number '{}'",
        text.substring(result.start(), result.end())
      );
      return "";
    });

  }
}
