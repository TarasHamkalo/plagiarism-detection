package dev.taras.hamkalo.plagdet.preprocessing.cleaner;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class Preprocessors implements Preprocessor {

  @NonNull
  List<Preprocessor> preprocessors;

  @Override
  public String getName() {
    return "Preprocessors agregator";
  }

  @Override
  public String process(String text) {
    var cleaned = text;
    for (var preprocessor : preprocessors) {
      cleaned = preprocessor.process(cleaned);
    }

    return cleaned;
  }
}
