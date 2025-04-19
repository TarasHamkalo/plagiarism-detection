package dev.taras.hamkalo.plagdet.preprocessing.annotators;

import dev.taras.hamkalo.plagdet.preprocessing.AnnotationUtils;
import dev.taras.hamkalo.plagdet.preprocessing.annotations.PlagdetAnnotations;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.util.CoreMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Goal of this class, is to create meaningful "sentences" from list of previously tokenized
 * lines. Problem it should solve, is that a lot of plag cases using sentence merging or
 * expansion.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TextBlockSentenceSplitter implements Annotator {

  private static final Set<String> CLAUSE_CONJUCTIONS = Set.of(
    "že", "aby", "ak", "ako", "akoby", "akonáhle", "akože", "ale", "alebo",
    "ani", "avšak", "ač", "ačkoľvek", "až", "ba", "hoci", "keby", "keď", "keďže",
    "lenže", "než", "pokiaľ", "pokým", "pretože", "pričom", "sotva",
    "zatiaľčo", "zatiaľ čo", "kým", "kýmže", "nakoľko", "lebo", "ibaže",
    "kdežto", "kde", "respektíve"
  );

  private static final String CLAUSE_SENTENCE_BOUNDARY_REGEX = String.format(
    "^%s$", CLAUSE_CONJUCTIONS.stream().map(Pattern::quote).collect(Collectors.joining("|"))
  );

  private static final String BASIC_SENTENCE_BOUNDARY = "(?:[a-zA-Z]\\.)|[.。]|[!?！？]+";

  private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^([^-=:]{1,50})[-=:](.{1,70})$");

  private static final Pattern PROGRAM_RUN_PATTERN = Pattern.compile("^(?:\\w+)?\\s*(?:<[\\w\\s]+>\\s*)+\\s*$");

  private static final int MAX_SENT_LENGTH_WORD = 15; // counting for small words which will be lately removed

  private static final int MIN_SENT_LENGTH_WORD = 5;

  private static final int MIN_LIST_ITEMS_TO_RECORD = 1;

  @NotNull WordToSentenceProcessor<CoreLabel> basicWsp;

  @NotNull WordToSentenceProcessor<CoreLabel> clauseWsp;

  public TextBlockSentenceSplitter() {
    this.basicWsp = new WordToSentenceProcessor<>(
      BASIC_SENTENCE_BOUNDARY, WordToSentenceProcessor.NewlineIsSentenceBreak.NEVER, false
    );
    this.clauseWsp = new WordToSentenceProcessor<>(
      CLAUSE_SENTENCE_BOUNDARY_REGEX, WordToSentenceProcessor.NewlineIsSentenceBreak.NEVER, false
    );
  }

  @Override
  public void annotate(Annotation annotation) {
    log.trace("Sentence length splitter annotator started");
    List<CoreMap> newSentences = new ArrayList<>();
    var sentences = AnnotationUtils.sentences(annotation);
    var validSentences = validateSentences(sentences);

    int sentencesSize = sentences.size();
    for (int i = 0; i < sentencesSize; ) {
      CoreMap blockBegin = sentences.get(i);
      if (validSentences.contains(i)) {
        newSentences.add(blockBegin);
        i++;
        continue;
      }

      if (i == sentencesSize - 1) {
        newSentences.add(blockBegin);
        break;
      }

      int textBlockId = AnnotationUtils.textBlockId(blockBegin);
      var blockTokens = new ArrayList<CoreLabel>(AnnotationUtils.tokens(blockBegin));
      int j = i + 1;
      for (; j < sentencesSize; j++) {
        if (validSentences.contains(j)) {
          break;
        }

        CoreMap next = sentences.get(j);
        if (AnnotationUtils.textBlockId(next) == textBlockId) {
          blockTokens.addAll(AnnotationUtils.tokens(next));
        } else {
          break;
        }
      }

      i = j;
      List<List<CoreLabel>> blockSentencesTokens = sentenceSplit(blockTokens);
      for (var blockSentenceTokens : blockSentencesTokens) {
        var blockSentence = AnnotationUtils.createSentence(blockSentenceTokens);
        AnnotationUtils.copy(PlagdetAnnotations.TextBlockId.class, blockSentence, blockBegin);
        newSentences.add(blockSentence);
      }
    }
    annotation.set(CoreAnnotations.SentencesAnnotation.class, newSentences);
  }

  private @NotNull List<List<CoreLabel>> sentenceSplit(@NotNull List<CoreLabel> tokens) {
    List<List<CoreLabel>> basicSplits = basicWsp.process(tokens);
    List<List<CoreLabel>> validatedSplits = new ArrayList<>(basicSplits.size());
    for (var split : basicSplits) {
      var wordTokens = AnnotationUtils.wordTokens(split);
      if (wordTokens.size() > MAX_SENT_LENGTH_WORD) {
        var clauseSentences = clauseWsp.process(split);
        validatedSplits.addAll(
          AnnotationUtils.mergeToMinLength(clauseSentences, MIN_SENT_LENGTH_WORD)
        );
      } else {
        validatedSplits.add(split);
      }
    }

    return validatedSplits;
  }

  /**
   * Checks each sentence whether it is properly split. Considers list item sentences
   *
   * @return set of valid sentences
   */
  public @NotNull Set<Integer> validateSentences(@NotNull List<CoreMap> sentences) {
    Set<Integer> validSentences = new HashSet<>(sentences.size() / 2);
    List<Integer> listItemsSentences = new ArrayList<>();
    int sentencesSize = sentences.size();
    for (int i = 0; i < sentencesSize; i++) {
      var text = sentences.get(i).get(CoreAnnotations.TextAnnotation.class);
      if (LIST_ITEM_PATTERN.matcher(text).matches()) {
        listItemsSentences.add(i);
        continue;
      }

      if (listItemsSentences.size() >= MIN_LIST_ITEMS_TO_RECORD) {
        validSentences.addAll(listItemsSentences);
        listItemsSentences.clear();
      }

      if (PROGRAM_RUN_PATTERN.matcher(text).matches()) {
        validSentences.add(i);
        continue;
      }

      if (isHeading(text)) {
        validSentences.add(i);
      }
    }

    return validSentences;
  }

  public boolean isHeading(@NotNull String text) {
    char first = text.charAt(0);
    return text.length() < 50 && (Character.isUpperCase(first) || Character.isDigit(first));
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Set.of();
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Set.of(
      CoreAnnotations.TokensAnnotation.class,
      CoreAnnotations.SentencesAnnotation.class,
      PlagdetAnnotations.TextBlockId.class
    );
  }
}
