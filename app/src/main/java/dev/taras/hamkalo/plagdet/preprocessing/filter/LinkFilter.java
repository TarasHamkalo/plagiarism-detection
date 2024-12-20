package dev.taras.hamkalo.plagdet.preprocessing.filter;


import dev.taras.hamkalo.plagdet.preprocessing.Token;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;

import java.util.regex.Pattern;

public class LinkFilter extends TokenFilter {
  // Remove URLs (com, org, net), emails, DOIs and citation tags
  public static final Pattern LINK_PATTERN = Pattern.compile(
    "(http\\S+|www\\S+|@\\S+|\\bdoi:\\S+\\b|\\[\\d+])|\\b\\S+\\.(org|net|com)\\S*\\b",
    Pattern.MULTILINE
  );

  @Override
  protected boolean doValidate(Token token) {
    if (Token.TokenType.TEXT.equals(token.getType())) {
      return !LINK_PATTERN.matcher(token.getContent()).matches();
    }

    return true;
  }
}
