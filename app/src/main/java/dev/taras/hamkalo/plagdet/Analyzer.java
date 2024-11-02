package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.preprocessing.cleaner.Preprocessor;
import dev.taras.hamkalo.plagdet.preprocessing.filter.TokenFilter;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import dev.taras.hamkalo.plagdet.preprocessing.transformation.Transformer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// TODO: normal implementation
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  List<DocumentProcessor> documentProcessors = new ArrayList<>();

  ContentReader contentReader;

  Preprocessor preprocessor;

  Transformer transformer;

  TokenFilter tokenFilter;

  public String analyze(List<String> paths) {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize");
//    props.setProperty("tokenize.options", "splitHyphenated=true");

//    props.setProperty("tokenize.whitespace", "false"); // This ensures diacritics are not stripped as part of whitespace handling
//    props.setProperty("tokenize.options", "normalizeParentheses=false,normalizeOtherBrackets=false");

    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    List<DocumentProcessor> documentProcessors = new ArrayList<>();
    for (String path : paths) {
      documentProcessors.add(
        new DocumentProcessor(
          path,
          contentReader,
          preprocessor,
          transformer,
          tokenFilter,
          pipeline
        )
      );
    }

    for (var documentProcessor : documentProcessors) {
      documentProcessor.preprocess();
      var documentEntry = documentProcessor.transform();
      for (var token : documentEntry.getTokens()) {
        System.out.println(token);
      }
    }

    return "analysis done";
  }

}
