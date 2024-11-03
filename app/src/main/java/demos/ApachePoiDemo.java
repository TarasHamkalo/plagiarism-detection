package demos;

import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

//https://mkyong.com/java/java-read-and-write-microsoft-word-with-apache-poi/
public class ApachePoiDemo {

  public static void main(String[] args) throws IOException {
    String fileName = "20.docx";
    try (XWPFDocument doc = new XWPFDocument(
      Files.newInputStream(Paths.get(fileName)))) {
      System.out.println(doc.getPackage());
      System.out.println(doc.getPart());
      POIXMLProperties.CoreProperties coreProperties = doc.getProperties().getCoreProperties();
      DocumentSummaryInformation documentSummaryInformation = new DocumentSummaryInformation();

      POIXMLProperties properties = doc.getProperties();
      POIXMLProperties.ExtendedProperties extProps = properties.getExtendedProperties();
      Optional.ofNullable(extProps.getTemplate()).ifPresent(template -> System.out.println("Template: " + template));
      Optional.ofNullable(extProps.getPages()).ifPresent(pages -> System.out.println("Page Count: " + pages));
      Optional.ofNullable(extProps.getWords()).ifPresent(words -> System.out.println("Word Count: " + words));
      Optional.ofNullable(extProps.getCharacters()).ifPresent(characters -> System.out.println("Character Count: " + characters));
      Optional.ofNullable(extProps.getTotalTime()).ifPresent(time -> System.out.println("Total Edit Time (minutes): " + time));
      System.out.println(doc.getProperties().getCustomProperties().getUnderlyingProperties().getPropertyList());
//      System.out.println(coreProperties.getCreator());
//      System.out.println(coreProperties.getUnderlyingProperties());
    }
//    String fileName = "hello.docx";
//    readWhole(fileName);
//    readByParagraph(fileName);
//    tableIterator(fileName);

  }

  private static void tableIterator(String fileName) throws IOException {
    try (XWPFDocument doc = new XWPFDocument(
        Files.newInputStream(Paths.get(fileName)))) {

    /*XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
    String docText = xwpfWordExtractor.getText();
    System.out.println(docText);*/

    Iterator<IBodyElement> docElementsIterator = doc.getBodyElementsIterator();

    //Iterate through the list and check for table element type
    while (docElementsIterator.hasNext()) {
        IBodyElement docElement = docElementsIterator.next();
        if ("TABLE".equalsIgnoreCase(docElement.getElementType().name())) {
            //Get List of table and iterate it
            List<XWPFTable> xwpfTableList = docElement.getBody().getTables();
            for (XWPFTable xwpfTable : xwpfTableList) {
                System.out.println("Total Rows : " + xwpfTable.getNumberOfRows());
                for (int i = 0; i < xwpfTable.getRows().size(); i++) {
                    for (int j = 0; j < xwpfTable.getRow(i).getTableCells().size(); j++) {
                        System.out.println(xwpfTable.getRow(i).getCell(j).getText());
                    }
                }
            }
        }
    }

}
  }

  private static void readByParagraph(String fileName) throws IOException {
    try (XWPFDocument doc = new XWPFDocument(
      Files.newInputStream(Paths.get(fileName)))) {

      // output the same as 8.1
      List<XWPFParagraph> list = doc.getParagraphs();
      for (XWPFParagraph paragraph : list) {
        System.out.println(paragraph.getElementType());
        System.out.println(paragraph.getText());
      }

    }
  }

  private static void readWhole(String fileName) throws IOException {
    try (XWPFDocument doc = new XWPFDocument(
      Files.newInputStream(Paths.get(fileName)))) {

      XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
      String docText = xwpfWordExtractor.getText();
      System.out.println(docText);

      // find number of words in the document
      long count = Arrays.stream(docText.split("\\s+")).count();
      System.out.println("Total words: " + count);

    }
  }

  private static void createDocx() throws IOException {
    String fileName = "hello.docx";

    try (XWPFDocument doc = new XWPFDocument()) {

      // create a paragraph
      XWPFParagraph p1 = doc.createParagraph();
      p1.setAlignment(ParagraphAlignment.CENTER);

      // set font
      XWPFRun r1 = p1.createRun();
      r1.setBold(true);
      r1.setItalic(true);
      r1.setFontSize(22);
      r1.setFontFamily("New Roman");
      r1.setText("I am first paragraph.");

      // save it to .docx file
      try (FileOutputStream out = new FileOutputStream(fileName)) {
        doc.write(out);
      }

    }
  }
}
