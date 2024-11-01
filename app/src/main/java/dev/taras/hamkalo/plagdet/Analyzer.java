package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.documents.DocumentProcessor;
import dev.taras.hamkalo.plagdet.preprocessing.Preprocessor;
import dev.taras.hamkalo.plagdet.readers.ContentReader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

// TODO: normal implementation
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Analyzer {

  List<DocumentProcessor> documentProcessors = new ArrayList<>();

  ContentReader contentReader;

  Preprocessor preprocessor;

  public String analyze(List<String> paths) {
    List<DocumentProcessor> documentProcessors = new ArrayList<>();
    for (String path : paths) {
      documentProcessors.add(new DocumentProcessor(path, contentReader, preprocessor));
    }

    for (var documentProcessor : documentProcessors) {
      var documentEntry = documentProcessor.preprocess();
      System.out.println(documentEntry.getNormalizedContent());
    }

    return "analysis done";
  }

}
