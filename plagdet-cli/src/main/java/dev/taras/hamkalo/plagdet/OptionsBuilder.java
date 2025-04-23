package dev.taras.hamkalo.plagdet;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OptionsBuilder {

  Options options = new Options();

  @Contract(" -> new")
  public static @NotNull OptionsBuilder builder() {
    return new OptionsBuilder();
  }

  public @NotNull OptionsBuilder qualifiedRequired(
    @NotNull String opt,
    @NotNull String longOpt,
    @NotNull String description,
    @Nullable Object defaultValue
  ) {
    options.addOption(
      Option.builder(opt)
        .required()
        .hasArg(true)
        .longOpt(longOpt)
        .desc(
          defaultValue == null ?
            description : String.format("%s Default [%s]", description, defaultValue)
        )
        .build()
    );
    return this;
  }

  public @NotNull OptionsBuilder noArg(
    @NotNull String opt,
    @NotNull String description,
    @Nullable Object defaultValue
  ) {
    options.addOption(
      Option.builder(opt)
        .hasArg(false)
        .desc(
          defaultValue == null ?
            description : String.format("%s Default [%s]", description, defaultValue)
        )
        .build()
    );
    return this;
  }

  public @NotNull OptionsBuilder optional(
    @NotNull String opt,
    @NotNull String description,
    @Nullable Object defaultValue
  ) {
    options.addOption(
      Option.builder(opt)
        .hasArg(true)
        .desc(
          defaultValue == null ?
            description : String.format("%s Default [%s]", description, defaultValue)
        )
        .build()
    );
    return this;
  }

  public @NotNull OptionsBuilder qualified(
    @NotNull String opt,
    @NotNull String longOpt,
    @NotNull String description,
    @Nullable Object defaultValue
  ) {
    options.addOption(
      Option.builder(opt)
        .hasArg(true)
        .longOpt(longOpt)
        .desc(
          defaultValue == null ?
            description : String.format("%s Default [%s]", description, defaultValue)
        )
        .build()
    );
    return this;
  }

  public @NotNull Options build() {
    return options;
  }
}
