package dev.taras.hamkalo.plagdet.io.report.model;

import dev.taras.hamkalo.plagdet.documents.FileData;
import dev.taras.hamkalo.plagdet.documents.markings.SpecialMarking;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Value
@Builder
public class DocumentEntryJsonModel {

  boolean isIndexed;

  @NotNull Integer id;

  @NotNull FileData fileData;

  @NotNull List<String> pairIds;

  @NotNull List<SpecialMarking> markings;
}
