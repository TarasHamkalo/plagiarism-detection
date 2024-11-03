package dev.taras.hamkalo.plagdet.preprocessing.cleaner;

// TODO: add tags removal
//Token(content=#include, type=TEXT, start=18220, end=18228)
//Token(content=<errno.h>, type=TEXT, start=18229, end=18238)
// TODO: hyphenated words regexsub(r'([A-Za-z])- ([a-z])', r'\1\2', text)
// TODO: replace links with spaces to preserve position
// TODO: move to core nlp filters
public interface Preprocessor {

  String getName();

  String process(String text);

}
