package dev.taras.hamkalo.plagdet.readers;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WordDocumentReader implements ContentReader {

  @Override
  public String getContent(Path path) throws ContentReadException {
    try (XWPFDocument doc = new XWPFDocument(
      Files.newInputStream(path))) {

      XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
      return xwpfWordExtractor.getText();

    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }

  }

  @Override
  public boolean supports(String extension) {
    return "DOCX".equals(extension.toUpperCase());
  }

}
