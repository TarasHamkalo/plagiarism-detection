package demos;

import dev.taras.hamkalo.plagdet.comparison.ngram.Ngram;
import lombok.SneakyThrows;
import org.apache.poi.xwpf.usermodel.XWPFComments;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComments;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

public class WordAnnotatorDemo {

  public void createAnnotatedWordDocument(String inputFilePath, String outputFilePath, List<Ngram> matches) throws Exception {
    FileInputStream fis = new FileInputStream(inputFilePath);
    XWPFDocument document = new XWPFDocument(fis);

    for (Ngram match : matches) {
      addCommentToMatchedText(document, match, "Matched n-gram: " + match.getContentStrings());
    }

    try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
      document.write(fos);
    }
    fis.close();
    System.out.println("Document saved with comments at " + outputFilePath);
  }

  private static void addCommentToMatchedText(XWPFDocument document, Ngram match, String commentText) throws Exception {
    int currentOffset = 0;

    for (XWPFParagraph paragraph : document.getParagraphs()) {
      for (XWPFRun run : paragraph.getRuns()) {
        String runText = run.getText(0);
        if (runText == null) continue;

        int runStart = currentOffset;
        int runEnd = currentOffset + runText.length();
        // Check if the match falls within this run
        if (match.getStart() >= runStart && match.getEnd() <= runEnd) {
          // Split the run text to isolate the match
          String beforeMatch = new String(runText.toCharArray(), 0, match.getStart() - runStart);
          String matchedText = new String(runText.toCharArray(), match.getStart(), match.getEnd() - match.getStart());
          String afterMatch = new String(runText.toCharArray(), match.getEnd(), runEnd - match.getEnd());

          // Update the run text
          run.setText(beforeMatch, 0);

          XWPFRun matchRun = appendCommentedRun(document, paragraph, run, matchedText);

          // after text
          XWPFRun afterRun = paragraph.insertNewRun(paragraph.getRuns().indexOf(matchRun) + 1);
          afterRun.setText(afterMatch);

          return;
        }

        currentOffset = runEnd;
      }
    }
  }

  private static XWPFRun appendCommentedRun(XWPFDocument document, XWPFParagraph paragraph, XWPFRun run, String matchedText) {
    XWPFComments comments = document.createComments();
    CTComments ctComments = comments.getCtComments();
    CTComment ctComment = ctComments.addNewComment();
    ctComment.setAuthor("Auto comment");
    ctComment.setDate(Calendar.getInstance());
    ctComment.setId(BigInteger.valueOf(10000));

    CTP ctP = paragraph.getCTP();

    CTMarkupRange commentRangeStart = ctP.addNewCommentRangeStart();
    commentRangeStart.setId(BigInteger.valueOf(10000));

    XWPFRun matchRun = paragraph.insertNewRun(paragraph.getRuns().indexOf(run) + 1);
    matchRun.setText(matchedText);

    CTMarkupRange commentRangeEnd = ctP.addNewCommentRangeEnd();
    commentRangeEnd.setId(BigInteger.valueOf(10000));

    ctP.addNewR().addNewCommentReference().setId(BigInteger.valueOf(10000));
    return matchRun;
  }


  @SneakyThrows
  public static void main(String[] args) {
    WordAnnotatorDemo demo = new WordAnnotatorDemo();
    var inputFile = "/home/taras-hamkalo/repositories/plagiarism-detection/app/src/main/resources/files/2013_2014_Lipka_Adam_217_11_dokumentacia_-Dokumentácia_Adam_Lipka1.docx";

    var ngram = Ngram.builder().contentStrings(List.of("technick, univerzit, kosic")).start(0).end(32).build();
    demo.createAnnotatedWordDocument(inputFile, "output2.docx", List.of(ngram));
  }

//
//  private static void addCommentToRun(XWPFDocument document, XWPFRun matchRun, String commentText) {
//    var comments = document.createComments();
//    var comment = comments.createComment(BigInteger.valueOf(comments.getComments().size() + 1));
//    var commentId = comment.getCtComment().getId();
//
//    comment.setAuthor("Auto commenter");
//
//    // runs
//    var runCtr = matchRun.getCTR();
//    CTP runCtp = (CTP) runCtr;
//
//    // placing a comment
//    var commentStart = runCtp.addNewCommentRangeStart();
//    var commentEnd = runCtp.addNewCommentRangeEnd();
//    commentStart.setId(commentId);
//    commentEnd.setId(commentId);
//
//    runCtr.addNewCommentReference().setId(commentId);
//  }
//        byte[] runBytes = runText.getBytes(StandardCharsets.UTF_8);
//        String beforeMatch = new String(runBytes, 0, match.getStart() - runStart, StandardCharsets.UTF_8);
//        String matchedText = new String(runBytes, match.getStart() - runStart, match.getEnd() - match.getStart(), StandardCharsets.UTF_8);
//        String afterMatch = new String(runBytes, match.getEnd() - runStart, runBytes.length - (match.getEnd() - runStart), StandardCharsets.UTF_8);
//  private static void addCommentToRun(XWPFDocument document, XWPFRun run, String commentText) throws Exception {
//    String commentAuthor = "AutoCommenter";
//
//    // Create the comments part if not already present
//    XWPFComments comments = document.getDocComments();
//    if (comments == null) {
//      comments = document.createComments();
//    }
//
//    // Create a new comment
//    CTComments ctComments = comments.getCtComments();
//    CTComment ctComment = ctComments.addNewComment();
//    BigInteger commentId = BigInteger.valueOf(ctComments.getCommentList().size() + 1);
//    ctComment.setId(commentId);
//    ctComment.setAuthor(commentAuthor);
////    ctComment.
//
//    // Link the comment to the run
//    var runCtr = run.getCTR();
//    CTP runCtp = (CTP) runCtr.getDomNode().getParentNode();
//    CTPPr pPr = runCtp.getPPr();
//    if (pPr == null) {
//      pPr = runCtp.addNewPPr();
//    }
//
//    CTMarkupRange startRange = runCtr.addNewCommentRangeStart();
//    startRange.setId(commentId);
//    CTMarkupRange endRange = runCtr.addNewCommentRangeEnd();
//    endRange.setId(commentId);
//
//    // Add comment text
//    runCtr.addNewCommentReference().setId(commentId);
//  }
//
//  private static void addCommentToRun(XWPFDocument document, XWPFRun run, String commentText) {
//    XWPFParagraph paragraph = (XWPFParagraph) run.getParent();
//    String commentAuthor = "AutoCommenter";
//
//    // Create the comment
//    XWPFComments comment = document.createComments();
//    var comm = comment.createComment(BigInteger.ONE); 
//    comm.setAuthor(commentAuthor);
//    comm.setText(commentText);
//
//    // Link the comment to the run
//    CTMarkupRange start = run.getCTR().addNewCommentReference();
//    addNewCommentRangeStart();
//    start.setId(comment.getId());
//    CTMarkupRange end = run.getCTR().addNewCommentRangeEnd();
//    end.setId(comment.getId());
//  }
}