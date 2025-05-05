package dev.taras.hamkalo.plagdet.io;

import dev.taras.hamkalo.plagdet.exception.PlagdetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;


@Slf4j
@RequiredArgsConstructor
public class ZipArchiver {

  public static void zipDirectory(
    @NotNull Path tempDir,
    @NotNull String outputName
  ) throws IOException {
    try (OutputStream fos = new FileOutputStream(outputName);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         ArchiveOutputStream<ZipArchiveEntry> aos = new ZipArchiveOutputStream(bos);
         Stream<Path> paths = Files.walk(tempDir)
    ) {
      paths.forEach(srcPath -> {
        var entryName = tempDir.relativize(srcPath).toString();
        try {
          if (!Files.isDirectory(srcPath)) {
            ZipArchiveEntry ze = new ZipArchiveEntry(srcPath.toFile(), entryName);
            aos.putArchiveEntry(ze);
            aos.write(Files.readAllBytes(srcPath));
            aos.closeArchiveEntry();
          } else {
            ZipArchiveEntry dirEntry = new ZipArchiveEntry(
              entryName.endsWith("/") ? entryName : entryName + "/"
            );
            aos.putArchiveEntry(dirEntry);
            aos.closeArchiveEntry();
          }
        } catch (IOException e) {
          throw new PlagdetException("Could not create zip entry", e);
        }
      });
    }
  }
}