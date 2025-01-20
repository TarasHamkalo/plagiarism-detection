package dev.taras.hamkalo.plagdet.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Optional;

@Value
@Builder
@AllArgsConstructor
public class Metadata {

  public static final Metadata NONE = new Metadata("Unknown", null, (long) 0);

  String creator;

  Date creationDate;

  Long totalEditTime;

}
