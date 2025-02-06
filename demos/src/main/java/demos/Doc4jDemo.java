package demos;

import lombok.SneakyThrows;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.Text;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class Doc4jDemo {

  @SneakyThrows
  public static void main(String[] args) {
    File doc = new File("20.docx");
    File out = new File("20.txt");
    WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage
      .load(doc);

    MainDocumentPart mainDocumentPart = wordMLPackage
      .getMainDocumentPart();
    String textNodesXPath = "//w:t";
    List<Object> textNodes = mainDocumentPart
      .getJAXBNodesViaXPath(textNodesXPath, true);
    FileWriter fileWriter = new FileWriter(out);
    for (Object obj : textNodes) {
      Text text = (Text) ((JAXBElement) obj).getValue();
      String textValue = text.getValue();
      fileWriter.write(textValue + "\n");
      System.out.println(textValue);
    }
  }

  private static void createDocx() throws Docx4JException {
    WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
    MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
    mainDocumentPart.addStyledParagraphOfText("Title", "Hello World!");
    mainDocumentPart.addParagraphOfText("Welcome To Baeldung");
    File exportFile = new File("welcome.docx");
    wordPackage.save(exportFile);
  }

}
