package dev.taras.hamkalo.plagdet.io;

import lombok.Value;

import java.util.Date;

@Value
public class Metadata {

  public static final Metadata NONE = new Metadata("Unknown", null, (long) -1);

  String authorName;

  Date creationDate;

  Long totalEditTime;

}
