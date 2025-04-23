package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.io.exception.FileDataSourceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ArchiveExtractor {

  /**
   * This field limit the count of directories on the path to otherwise valid entry
   */
  public static final int MAX_NESTED_DIRS_ALLOWED = 1;

  public static Map<String, File> extract(
    @NotNull File file,
    @Nullable Set<String> allowedFormats
  ) throws FileDataSourceException {

    var result = new HashMap<String, File>();
    try (InputStream fis = new FileInputStream(file);
         BufferedInputStream bis = new BufferedInputStream(fis);
         ArchiveInputStream<ZipArchiveEntry> ais = new ZipArchiveInputStream(bis)) {

      Path tempDir = Files.createTempDirectory("extracted_");
      tempDir.toFile().deleteOnExit();
      Path extractDest = tempDir.resolve(FilenameUtils.getBaseName(file.getName()));
      extractDest.toFile().deleteOnExit();
      ArchiveEntry entry;
      while ((entry = ais.getNextEntry()) != null) {
        if (!isValidEntry(entry, allowedFormats)) {
          log.trace("Dropping zip entry {}", entry.getName());
          continue;
        }

        File newFile = new File(extractDest.toFile(), entry.getName());
        File parentFile = newFile.getParentFile();
        parentFile.mkdirs();
        parentFile.deleteOnExit();
        newFile.deleteOnExit();
        try (var fos = new FileOutputStream(newFile)) {
          IOUtils.copy(ais, fos);
        }
        result.put(entry.getName(), newFile);
      }

    } catch (IOException e) {
      throw new FileDataSourceException(e);
    }

    return result;
  }

  private static boolean isValidEntry(@NotNull ArchiveEntry entry, @Nullable Set<String> allowedFormats) {
    String baseName = FilenameUtils.getBaseName(entry.getName());

    if (entry.isDirectory() || entry.getName().contains("../") || baseName.startsWith(".")) {
      return false;
    }

    if (entry.getName().chars().filter(c -> c == '/').count() > MAX_NESTED_DIRS_ALLOWED) {
      return false;
    }

    if (allowedFormats == null) {
      return true;
    }

    return allowedFormats.contains(FilenameUtils.getExtension(entry.getName()));
  }

}