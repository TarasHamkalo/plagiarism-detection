package dev.taras.hamkalo.plagdet.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Optional;

@Value
@Builder
@AllArgsConstructor
public class Metadata {

  public static final Metadata NONE = new Metadata(null, null, null, 0L, 0, null, null);

  @Nullable String creator;

  @Nullable String modifier;

  @Nullable Date creationDate;

  @NotNull Long totalEditTime;

  @NotNull Integer revisionsNumber;

  @Nullable Date modificationDate;

  @Nullable Date lastPrinted;

}
