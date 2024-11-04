package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.documents.Metadata;
import dev.taras.hamkalo.plagdet.util.Utils;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.antlr.stringtemplate.PathGroupLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileSystemIndex implements DocumentIndex {

  public static final String FS_INDEX_PATH = "SIMPLE_FS_INDEX/";

  @Override
  @SneakyThrows
  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
    var ext = Utils.getExtension(documentEntry.getPath());
    // TODO: should catch that, but actually not proceed
    var target = Path.of(getIndexLocation(documentEntry, ext));
    if (target.toFile().exists()) {
      // TODO: replace existing ?
      log.warn("{} document already exists in FS as {}", documentEntry.getPath(), target);
      return documentEntry;
    }

    var indexLocation = Files.copy(
      documentEntry.getPath(), target
    );

    // TODO: change to final field and wither
    documentEntry.setPath(indexLocation);
    return documentEntry;
  }

  private DocumentEntry pushToIndexTokenized(DocumentEntry documentEntry) {
    try (var writer = new FileWriter(getIndexLocation(documentEntry, "txt"))) {
      var content = documentEntry.getContentFromTokens();
      writer.append(content);
      return documentEntry;
    } catch (IOException e) {
      // TODO: IOException handling
      throw new RuntimeException(e);
    }
  }

  private String getIndexLocation(DocumentEntry documentEntry, String ext) {
    // TODO: not tested whether string hash is not randomized
    return FS_INDEX_PATH + documentEntry.getId() + "." + ext;
  }

  // TODO: renaming, probably need to go into other interface
  /**
   *
   * @param documentEntry
   * @return documentEntry filled with content found in fs index if not exist then throw
   */
  @Override
  @SneakyThrows
  public List<DocumentEntry> retrieveSimilarByContent(DocumentEntry documentEntry) {
    var ext = Utils.getExtension(documentEntry.getPath());
    var location = getIndexLocation(documentEntry, ext);
    // TODO: location not exist check, corrupted index!
    String fileContent = FileUtils.readFileToString(new File(location));
    documentEntry.setContent(fileContent);

    return List.of(documentEntry);
  }

}
