package dev.taras.hamkalo.plagdet.index;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import dev.taras.hamkalo.plagdet.util.Utils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileSystemStore {

  public static final String FS_INDEX_PATH = "SIMPLE_FS_INDEX/";

  @SneakyThrows
  public DocumentEntry addToStore(DocumentEntry documentEntry) {
    var ext = Utils.getExtension(Path.of(documentEntry.getContentPath()));
    // TODO: should catch that, but actually not proceed
    var target = Path.of(getIndexLocation(documentEntry, ext));
    if (target.toFile().exists()) {
      // TODO: replace existing ?
      log.warn("{} document already exists in FS as {}", documentEntry.getContentPath(), target);
      return documentEntry;
    }

    var storeLocation = Files.copy(
      Path.of(documentEntry.getContentPath()), target
    );

    // TODO: change to final field and wither
    documentEntry.setContentPath(storeLocation.toString());
    return documentEntry;
  }

  private String getIndexLocation(DocumentEntry documentEntry, String ext) {
    // TODO: not tested whether string hash is not randomized
    long hash = documentEntry.getContentPath().hashCode();
    return FS_INDEX_PATH + hash + "." + ext;
  }

}

//  // TODO: renaming, probably need to go into other interface
//  /**
//   *
//   * @param documentEntry
//   * @return documentEntry filled with content found in fs index if not exist then throw
//   */
//  @SneakyThrows
//  public DocumentEntry setPath(DocumentEntry documentEntry) {
//    // TODO: location not exist check, corrupted index!
//    String fileContent = contentReaders.getContent(documentEntry.getPath());
//    // TODO: readers -> to content -> preprocess
//    documentEntry.setContent(fileContent);
//
//    return List.of(documentEntry);
//  }
//
//private DocumentEntry pushToIndexTokenized(DocumentEntry documentEntry) {
//  try (var writer = new FileWriter(getIndexLocation(documentEntry, "txt"))) {
//    var content = documentEntry.getContentFromTokens();
//    writer.append(content);
//    return documentEntry;
//  } catch (IOException e) {
//    // TODO: IOException handling
//    throw new RuntimeException(e);
//  }
//}
