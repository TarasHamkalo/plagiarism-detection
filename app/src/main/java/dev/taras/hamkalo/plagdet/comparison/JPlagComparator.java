package dev.taras.hamkalo.plagdet.comparison;

import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.merging.MergingOptions;
import de.jplag.options.JPlagOptions;
import de.jplag.text.NaturalLanguage;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.util.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JPlagComparator {

  public static final String IPC_TXT_TEMPLATE = "/home/taras-hamkalo/repositories/plagiarism-detection/app/src/main/resources/ipc.txt";

  public void compare(DocumentEntry target, List<DocumentEntry> old) {
    Map<String, String> tempPathToOriginal = new HashMap<>();

    try {
      File oldSubmissionsDir = Files.createTempDirectory("jplag_tokens").toFile();
      System.out.println(
        "Temporary directory for old submissions created at: " + oldSubmissionsDir.getAbsolutePath()
      );

      for (DocumentEntry entry : old) {
        var fname = Utils.getFileName(Path.of(entry.getContentPath()));
        File tokenFile = new File(oldSubmissionsDir, fname + "_tokens.txt");
        tempPathToOriginal.put(tokenFile.getPath(), entry.getContentPath());
        try (FileWriter writer = new FileWriter(tokenFile)) {
          writer.append(entry.getContentFromTokens());
          System.out.printf(
            "Tokens for document %s written to: %s\n", fname, tokenFile.getAbsolutePath()
          );
        }
      }

      File targetSubmissionDir = Files.createTempDirectory("target").toFile();
      var fname = Utils.getFileName(Path.of(target.getContentPath()));
      File tokenFile = new File(targetSubmissionDir, fname + ".txt");
      tempPathToOriginal.put(tokenFile.getPath(), target.getContentPath());
      try (FileWriter writer = new FileWriter(tokenFile)) {
        writer.append(target.getContentFromTokens());
        System.out.printf(
          "Tokens for document target written to: %s\n", fname, tokenFile.getAbsolutePath()
        );
      }

      MergingOptions mergingOptions = new MergingOptions()
        .withMaximumGapSize(3)
        .withEnabled(true);

      JPlagOptions options = new JPlagOptions(new NaturalLanguage(), Set.of(targetSubmissionDir), Set.of())
        .withMergingOptions(mergingOptions)
        .withOldSubmissionDirectories(Set.of(oldSubmissionsDir))
        .withBaseCodeSubmissionDirectory(new File(IPC_TXT_TEMPLATE))
        .withMinimumTokenMatch(3);

      JPlagResult result = JPlag.run(options);
      printJPlagComparison(result.getAllComparisons(), tempPathToOriginal, target.getContentPath());

    } catch (IOException e) {
      System.err.println("Error creating temporary directory or writing tokens: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Error during JPlag comparison: " + e.getMessage());
    }
  }

  public void printJPlagComparison(List<JPlagComparison> comparisons,
                                   Map<String, String> tempPathToOriginal,
                                   String targetFile
  ) {
    System.out.println("Jplag comparisons result for file: " + targetFile);

    for (int i = 0; i < comparisons.size(); i++) {
      var comparison = comparisons.get(i);
      var other = tempPathToOriginal.get(comparison.firstSubmission().getRoot().toString());
      if (other.equals(targetFile)) {
        other = tempPathToOriginal.get(comparison.secondSubmission().getRoot().toString());
      }

      System.out.printf("Comparison %d:\n", i + 1);
      System.out.println("\tCompared to: " + other);
      System.out.println("\tAvg similarity: " + comparison.similarity());
      System.out.println("\tMax similarity: " + comparison.maximalSimilarity());
    }
  }

}
