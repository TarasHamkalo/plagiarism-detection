package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileSystemIndex implements DocumentIndex {

  String FS_INDEX_PATH = "SIMPLE_FS_INDEX/";

  @Override
  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
    var fname = documentEntry.getPath().getFileName().toString();
    fname = fname.substring(0, fname.lastIndexOf('.'));
    try (var writer = new FileWriter(FS_INDEX_PATH + fname + ".txt")) {
      var content = documentEntry.getContentFromTokens();
      writer.append(content);
      return documentEntry;
    } catch (IOException e) {
      // TODO: IOException handling
      throw new RuntimeException(e);
    }

  }

  @Override
  public List<DocumentEntry> retrieveSimilarByContent(DocumentEntry documentEntry) {
    return List.of();
  }

  @Override
  public DocumentEntry retrieveSimilarByMeta(Metadata metadata) {
    return null;
  }

}
