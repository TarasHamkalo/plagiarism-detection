package dev.taras.hamkalo.plagdet.index.metadata;

import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Date;


@Entity
@Getter
@Setter
@Table(name = "documents")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  // TODO: path which should point to the FS index document copy
  @Column(nullable = false, unique = true)
  String fileStoragePath;

  @Column(nullable = false, unique = false) // TODO: NOT UNIQUE
  String authorName;

  @Column(nullable = true)
  Date creationDate;

  @Column(nullable = true)
  Integer totalEditTime;

  public static DocumentEntity from(DocumentEntry documentEntry) {
    var meta = documentEntry.getMetadata();
    var contentPath = Path.of(documentEntry.getContentPath()).toAbsolutePath();
    return DocumentEntity.builder()
      .fileStoragePath(contentPath.toString())
      .authorName(meta.getAuthorName())
      .creationDate(meta.getCreationDate())
      .totalEditTime(meta.getTotalEditTime())
      .build();
  }
}
