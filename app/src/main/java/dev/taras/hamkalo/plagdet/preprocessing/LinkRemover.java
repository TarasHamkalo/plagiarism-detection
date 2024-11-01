package dev.taras.hamkalo.plagdet.preprocessing;


import java.util.regex.Pattern;

public class LinkRemover implements Preprocessor {
  // Remove URLs (com, org, net), emails, DOIs and citation tags
  public static final Pattern LINK_PATTERN = Pattern.compile(
    "(http\\S+|www\\S+|@\\S+|\\bdoi:\\S+\\b|\\[\\d+])|\\b\\S+\\.(org|net|com)\\S*\\b",
    Pattern.MULTILINE
  );

  @Override
  public String getName() {
    return "Link remover";
  }

  @Override
  public String process(String text) {
    var matcher = LINK_PATTERN.matcher(text);
    return matcher.replaceAll("");
  }

}
