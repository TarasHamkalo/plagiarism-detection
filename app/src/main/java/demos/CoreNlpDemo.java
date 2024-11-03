package demos;

import dev.taras.hamkalo.plagdet.readers.TextReader;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.nio.file.Path;
import java.util.Properties;

public class CoreNlpDemo {

  public static void main(String[] args) {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize");
    props.setProperty("tokenize.options", "splitHyphenated=true");
    // build pipeline
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    // create a document object
    var text = new TextReader().getContent(Path.of("/home/taras-hamkalo/repositories/plagiarism-detection/app/src/main/resources/11.txt"));
    CoreDocument doc = new CoreDocument(text);
    // annotate
    pipeline.annotate(doc);
    // display tokens
    for (CoreLabel tok : doc.tokens()) {
      System.out.println(String.format("%s\t%d\t%d", tok.word(), tok.beginPosition(), tok.endPosition()));
    }
  }
}
