package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DocDocumentReader implements ContentReader, MetadataReader {

  @Override
  public String getContent(Path path) throws ContentReadException {
    try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(path))) {
      WordExtractor extractor = new WordExtractor(doc);
      return extractor.getText();
    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }
  }

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {
    try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(path))) {
      SummaryInformation summaryInfo = doc.getSummaryInformation();

      String author = Optional.ofNullable(summaryInfo.getAuthor())
        .orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(summaryInfo.getLastAuthor())
        .orElse(Metadata.NONE.getModifier());
      var creationDate = summaryInfo.getCreateDateTime();
      var modificationDate = summaryInfo.getLastSaveDateTime();
      var totalEditTime = summaryInfo.getEditTime();
      var lastPrinted = summaryInfo.getLastPrinted();
      var revisionsNumber = summaryInfo.getRevNumber();

      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(creationDate)
        .modificationDate(modificationDate)
        .totalEditTime(totalEditTime)
        .lastPrinted(lastPrinted)
        .revisionsNumber(revisionsNumber != null ? Integer.parseInt(revisionsNumber) : 0)
        .build();
    } catch (Exception e) {
      throw new MetadataReadException(e.getMessage());
    }
  }

  @Override
  public boolean supports(String extension) {
    return "DOC".equalsIgnoreCase(extension);
  }
}
