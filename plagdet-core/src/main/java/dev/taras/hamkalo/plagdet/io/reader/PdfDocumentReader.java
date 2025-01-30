package dev.taras.hamkalo.plagdet.io.reader;

import dev.taras.hamkalo.plagdet.io.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.Optional;

public class PdfDocumentReader implements ContentReader, MetadataReader {

  @Override
  public String getContent(Path path) throws ContentReadException {
    try (PDDocument document = Loader.loadPDF(path.toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    } catch (IOException e) {
      throw new ContentReadException(e.getMessage());
    }
  }

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {
    try (PDDocument document = Loader.loadPDF(path.toFile())) {
      PDDocumentInformation info = document.getDocumentInformation();

      String author = Optional.ofNullable(info.getAuthor())
        .orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(info.getCustomMetadataValue("LastModifiedBy"))
        .orElse(Metadata.NONE.getModifier());
      var creationDate = info.getCreationDate() != null ? info.getCreationDate().getTime() : null;
      var modificationDate = info.getModificationDate() != null ? info.getModificationDate().getTime() : null;

      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(creationDate)
        .modificationDate(modificationDate)
        .lastPrinted(null)
        .revisionsNumber(0)
        .build();
    } catch (Exception e) {
      throw new MetadataReadException(e.getMessage());
    }
  }


  @Override
  public boolean supports(String extension) {
    return "PDF".equalsIgnoreCase(extension);
  }
}
