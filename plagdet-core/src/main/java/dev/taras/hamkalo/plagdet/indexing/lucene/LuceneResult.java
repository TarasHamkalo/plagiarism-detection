package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.io.FileData;

public record LuceneResult(FileData fileData, double score) {
}
