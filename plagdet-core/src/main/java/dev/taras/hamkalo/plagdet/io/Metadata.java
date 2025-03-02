package dev.taras.hamkalo.plagdet.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true) // json mapping ...
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
