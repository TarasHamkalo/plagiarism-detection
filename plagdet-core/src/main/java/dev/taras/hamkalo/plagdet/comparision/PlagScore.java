package dev.taras.hamkalo.plagdet.comparision;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlagScore {

  double score;

  Direction direction;

  List<String> comments;

}
