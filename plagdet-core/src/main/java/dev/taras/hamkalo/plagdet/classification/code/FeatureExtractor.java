package dev.taras.hamkalo.plagdet.classification.code;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class FeatureExtractor {
  public static final int FEATURE_VECTOR_SIZE = 13;

  private static final Pattern CAMEL_CASE = Pattern.compile("\\b[a-z]+[A-Z][a-zA-Z]*\\b");

  private static final Pattern SNAKE_CASE = Pattern.compile("\\b[a-zA-Z]+(_[a-zA-Z]+)*\\b");

  private static final Pattern NUMBER = Pattern.compile("\\d+");

  private static final Set<String> KEYWORDS = new HashSet<>(List.of(
    "#include", "#define", "int", "char", "float", "return", "void",
    "if", "else", "else-if", "while", "for", "true", "printf"
  ));

  public static double[] extractFeatures(String line) {
    if (line.isEmpty()) {
      return new double[FEATURE_VECTOR_SIZE];
    }

    String trim = line.trim();
    String[] tokens = trim.split("\\s+");
    List<String> tokensList = new ArrayList<>(Arrays.asList(tokens));

    double[] features = new double[FEATURE_VECTOR_SIZE];
    Map<Character, Integer> characterFrequencyMap = new HashMap<>();
    int lineLength = line.length();
    int letterCount = 0;
    int upperCaseCount = 0;
    for (int i = 0; i < lineLength; i++) {
      char ch = line.charAt(i);
      int freq = characterFrequencyMap.getOrDefault(ch, 0);
      characterFrequencyMap.put(ch, freq + 1);
      if (Character.isLetter(ch)) {
        letterCount++;
      }

      if (Character.isUpperCase(ch)) {
        upperCaseCount++;
      }
    }

    long symbolsCount = line.length() - letterCount;

    features[0] = Math.min(1, characterFrequencyMap.getOrDefault(';', 0));
    features[1] = trim.startsWith("//") ? 1 : 0;
    features[2] = tokens.length;
    features[3] = characterFrequencyMap.getOrDefault('{', 0);
    features[4] = characterFrequencyMap.getOrDefault('}', 0);
    features[5] = countCamelCase(tokensList);
    features[6] = characterFrequencyMap.getOrDefault(',', 0) +
                  characterFrequencyMap.getOrDefault('.', 0) +
                  characterFrequencyMap.getOrDefault('!', 0) +
                  characterFrequencyMap.getOrDefault('?', 0);
    features[7] = countNumbers(tokensList);
    features[8] = symbolsCount == 0 ? 0 : (double) letterCount / symbolsCount;
    features[9] = countSnake(tokensList);
    features[10] = countKeywords(tokensList);
    features[11] = upperCaseCount;
    features[12] = !trim.isEmpty() && Character.isUpperCase(trim.charAt(0)) ? 0 : 1;

    return features;
  }

  private static int countKeywords(@NotNull List<String> tokens) {
    int n = tokens.size();
    tokens.removeIf(KEYWORDS::contains);
    return n - tokens.size();
  }

  private static int countNumbers(@NotNull List<String> tokens) {
    int n = tokens.size();
    tokens.removeIf(s -> NUMBER.matcher(s).matches());
    return n - tokens.size();
  }

  private static int countCamelCase(@NotNull List<String> tokens) {
    int n = tokens.size();
    tokens.removeIf(s -> CAMEL_CASE.matcher(s).matches());
    return n - tokens.size();
  }

  private static int countSnake(@NotNull List<String> tokens) {
    int n = tokens.size();
    tokens.removeIf(s -> SNAKE_CASE.matcher(s).matches());
    return n - tokens.size();
  }
}
