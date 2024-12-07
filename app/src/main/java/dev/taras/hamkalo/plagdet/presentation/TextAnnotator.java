package dev.taras.hamkalo.plagdet.presentation;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.Fragment;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.io.BufferedWriter;
/**
 * Create a simple text file with content marked for each found fragment
 */
public class TextAnnotator {

  public void createAnnotatedDocuments(DocumentEntry first,
                                       DocumentEntry second,
                                       List<Fragment> sharedFragments) {

    writeAnnotated(first, sharedFragments, (f) -> new int[]{f.getFirstStart(), f.getFirstEnd()});
    writeAnnotated(second, sharedFragments, (f) -> new int[]{f.getSecondStart(), f.getSecondEnd()});
  }


  private void writeAnnotated(DocumentEntry document,
                              List<Fragment> sharedFragments,
                              Function<Fragment, int[]> posMapper) {

    sharedFragments.sort(Comparator.comparingInt(f -> posMapper.apply(f)[0]));
    StringBuilder annotatedContent = new StringBuilder(document.getContent());

    int offset = 0;

    for (Fragment fragment : sharedFragments) {
      int[] positions = posMapper.apply(fragment);
      int start = positions[0] + offset;
      int end = positions[1] + offset;

      annotatedContent.insert(end, String.format("</frag fid=%d>", fragment.getId()));
      offset += String.format("</frag fid=%d>", fragment.getId()).length();

      annotatedContent.insert(start, String.format("<frag fid=%d>", fragment.getId()));
      offset += String.format("<frag fid=%d>", fragment.getId()).length();
    }

    String outputPath = document.getContentPath() + "_annotated.txt";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
        writer.write(annotatedContent.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
