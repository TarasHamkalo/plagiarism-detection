package dev.taras.hamkalo.plagdet.io.reader;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.ClipperBridge;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.ClipperException;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextChunk;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.io.exception.ContentReadException;
import dev.taras.hamkalo.plagdet.io.exception.MetadataReadException;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

public class PdfDocumentReader implements DocumentReader {

  static {
    ClipperBridge.floatMultiplier = Math.pow(10, 10);
  }

  @Override
  public String getContent(Path path) throws ContentReadException {
    try (PdfReader reader = new PdfReader(path.toFile());
         PdfDocument pdfDoc = new PdfDocument(reader)) {

      StringBuilder text = new StringBuilder();
      int numPages = pdfDoc.getNumberOfPages();

      for (int i = 1; i <= numPages; i++) {

        LocationTextExtractionStrategy strategy = new LocationTextExtractionStrategy() {
          protected boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
            float dist = chunk.getLocation().distanceFromEndOf(previousChunk.getLocation());
            return dist < -chunk.getLocation().getCharSpaceWidth() ||
                   dist > chunk.getLocation().getCharSpaceWidth() / 4.0f;
          }
        };
        text.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy)).append("\n");
      }

      return text.toString().trim();
    } catch (com.itextpdf.io.exceptions.IOException | ClipperException | IOException e) {
      throw new ContentReadException("Failed to read PDF content: " + e.getMessage());
    }
  }

  @Override
  public Metadata getMetadata(Path path) throws MetadataReadException {
    try (PdfReader reader = new PdfReader(path.toFile());
         PdfDocument pdfDoc = new PdfDocument(reader)) {

      PdfDocumentInfo info = pdfDoc.getDocumentInfo();

      String author = Optional.ofNullable(info.getAuthor()).orElse(Metadata.NONE.getCreator());
      String modifier = Optional.ofNullable(info.getMoreInfo("LastModifiedBy"))
        .orElse(Metadata.NONE.getModifier());

      String creationDate = info.getMoreInfo("CreationDate");
      String modificationDate = info.getMoreInfo("ModDate");
      return Metadata.builder()
        .creator(author)
        .modifier(modifier)
        .creationDate(parsePdfDate(creationDate))
        .modificationDate(parsePdfDate(modificationDate))
        .totalEditTime(0L)
        .lastPrinted(null)
        .revisionsNumber(0)
        .build();
    } catch (com.itextpdf.io.exceptions.IOException | ClipperException | IOException e) {
      throw new MetadataReadException("Failed to read PDF metadata: " + e.getMessage());
    }
  }

  public static Date parsePdfDate(String pdfDate) {
    if (pdfDate == null) {
      return null;
    }
    if (pdfDate.startsWith("D:")) {
      pdfDate = pdfDate.substring(2);
    }

    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      return format.parse(pdfDate.substring(0, 14));
    } catch (ParseException e) {
      return null;
    }


  }

  @Override
  public boolean supports(String extension) {
    return "PDF".equalsIgnoreCase(extension);
  }
}
