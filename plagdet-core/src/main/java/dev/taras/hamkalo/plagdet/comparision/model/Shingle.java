package dev.taras.hamkalo.plagdet.comparision.model;

import edu.stanford.nlp.ling.CoreLabel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
public class Shingle {

  @NotNull
  String content;

  @NotNull
  @EqualsAndHashCode.Exclude
  Integer start;

  @NotNull
  @EqualsAndHashCode.Exclude
  Integer end;

  public static Shingle from(
    @NotNull List<CoreLabel> tokens,
    @NotNull Function<List<CoreLabel>, String> tokenMapper
  ) {
    return Shingle.builder()
      .start(tokens.get(0).beginPosition())
      .end(tokens.get(tokens.size() - 1).endPosition())
      .content(tokenMapper.apply(tokens))
      .build();
  }

  public static @NotNull String tokensToLemmaString(@NotNull List<CoreLabel> tokens) {
    return tokens.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
  }

  public static @NotNull String tokensToPosString(@NotNull List<CoreLabel> tokens) {
    return tokens.stream().map(CoreLabel::tag).collect(Collectors.joining(" "));
  }
}
