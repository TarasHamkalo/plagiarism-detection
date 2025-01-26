package dev.taras.hamkalo.plagdet;

import dev.taras.hamkalo.plagdet.comparision.comparator.MetadataComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.SentenceComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.ShingleComparator;
import dev.taras.hamkalo.plagdet.comparision.comparator.blocks.Embeddings;
import dev.taras.hamkalo.plagdet.documents.DocumentAnnotator;
import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import dev.taras.hamkalo.plagdet.indexing.lucene.LuceneIndex;
import dev.taras.hamkalo.plagdet.io.SubmissionsParser;
import dev.taras.hamkalo.plagdet.preprocessing.Pipeline;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Plagdet {

  Embeddings embeddings;

  SubmissionsParser submissionsParser;

  LuceneIndex luceneIndex;

  ComparisonPipeline comparisonPipeline;

  Pipeline pipeline;

  DocumentAnnotator documentAnnotator;

  public Plagdet(@NotNull PlagdetParameters plagdetParameters) {
    this.luceneIndex = this.initLucene(plagdetParameters.getLuceneIndexDirectoryPath());
    this.embeddings = new Embeddings(luceneIndex);
    this.comparisonPipeline = this.initComparisonPipeline(plagdetParameters, embeddings);
    this.submissionsParser = new SubmissionsParser();
  }

  private @NotNull ComparisonPipeline initComparisonPipeline(
    @NotNull PlagdetParameters plagdetParameters,
    @NotNull Embeddings embeddings
  ) {
//    return new ComparisonPipeline(
//
//      new MetadataComparator(plagdetParameters.getMetadataComparatorParameters()),
//      new ShingleComparator(plagdetParameters.getShingleComparatorParameters()),
//      new SentenceComparator(plagdetParameters.getSentenceComparatorParameters(), embeddings)
//    );
  }

  private @NotNull LuceneIndex initLucene(@Nullable String luceneIndexDirectoryPath) {
    try {
      if (luceneIndexDirectoryPath == null) {
        Path luceneTempDir = Files.createTempDirectory("temporary_lucene_index");
        return new LuceneIndex(luceneTempDir);
      }

      return new LuceneIndex(Path.of(luceneIndexDirectoryPath));
    } catch (IOException e) {
      throw new PlagdetException(
        String.format("Failed to init lucene with directory %s", luceneIndexDirectoryPath), e
      );
    }
  }
}
