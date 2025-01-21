package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DocxDocumentReader implements ContentReader, MetaReader{

  @Override
  public String getContent(Path path) throws ContentReadException {
    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
      ZipSecureFile.setMinInflateRatio(0);
      XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
      return xwpfWordExtractor.getText();

    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }

  }

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {
    try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
      var properties = doc.getProperties();
      var coreProps = properties.getCoreProperties();
      var extProps = properties.getExtendedProperties();


      String author = Optional.ofNullable(coreProps.getCreator())
        .orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(coreProps.getLastModifiedByUser())
        .orElse(Metadata.NONE.getModifier());
      var creationDate = coreProps.getCreated();
      var modificationDate = coreProps.getModified();
      int totalEditTime = extProps.getTotalTime();
      var lastPrinted = coreProps.getLastPrinted();
      String revisionsNumber = Optional.ofNullable(coreProps.getRevision())
        .orElse("0");

      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(creationDate)
        .modificationDate(modificationDate)
        .totalEditTime((long) totalEditTime)
        .lastPrinted(lastPrinted)
        .revisionsNumber(Integer.parseInt(revisionsNumber))
        .build();
    } catch (Exception e) {
      throw new MetadataReadException(e.getMessage());
    }

  }

  @Override
  public boolean supports(String extension) {
    return "DOCX".equalsIgnoreCase(extension);
  }

}
