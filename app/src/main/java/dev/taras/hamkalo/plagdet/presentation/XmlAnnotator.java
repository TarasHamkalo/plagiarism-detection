package dev.taras.hamkalo.plagdet.presentation;

import dev.taras.hamkalo.plagdet.comparison.ngram.v2.Fragment;
import dev.taras.hamkalo.plagdet.documents.DocumentEntry;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class XmlAnnotator {

  public void createXMLAnnotatedMatches(DocumentEntry first,
                                        DocumentEntry second,
                                        List<Fragment> sharedFragments) {

    StringBuilder xmlContent = new StringBuilder();
    xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlContent.append("<matches>\n");

    for (Fragment fragment : sharedFragments) {
      xmlContent.append(String.format("\t<frag fid=\"%d\">\n", fragment.getId()));

      xmlContent.append(String.format("\t\t<doc pos=[%d, %d] name=\"%s\">\n", fragment.getFirstStart(), fragment.getFirstEnd(), first.getContentPath()));
      xmlContent
        .append(first.getContent(), fragment.getFirstStart(), fragment.getFirstEnd())
        .append("\n");
      xmlContent.append("\t\t</doc>\n");

      xmlContent.append(String.format("\t\t<doc pos[%d, %d] name=\"%s\">\n", fragment.getSecondStart(), fragment.getSecondEnd(), first.getContentPath()));
      xmlContent
        .append(second.getContent(), fragment.getSecondStart(), fragment.getSecondEnd())
        .append("\n");

      xmlContent.append("\t\t</doc>\n");

      xmlContent.append("\t</frag>\n");
    }

    xmlContent.append("</matches>");

    String outputPath = "matches_annotated.xml";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
      writer.write(xmlContent.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
