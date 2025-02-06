package dev.taras.hamkalo.plagdet.index.metadata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

  List<DocumentEntity> findByAuthorName(String authorName);

//  List<DocumentEntity> findByCreationDate(String authorName);
}
