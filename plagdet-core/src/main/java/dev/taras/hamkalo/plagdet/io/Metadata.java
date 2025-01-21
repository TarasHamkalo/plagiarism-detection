package dev.taras.hamkalo.plagdet.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
@AllArgsConstructor
public class Metadata {

  public static final Metadata NONE = new Metadata("Unknown", "Unknown", null, 0L, 0, null, null);

  String creator;

  String modifier;

  Date creationDate;

  Long totalEditTime;

  Integer revisionsNumber;

  Date modificationDate;

  Date lastPrinted;

}
