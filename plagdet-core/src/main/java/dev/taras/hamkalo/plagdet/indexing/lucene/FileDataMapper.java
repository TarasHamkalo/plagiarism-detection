package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.FileData;
import org.apache.lucene.document.Document;

import static org.apache.lucene.document.Field.Store;

import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

public class FileDataMapper {


  public static void addId(int id, Document doc) {
    doc.add(new StringField(LuceneConstants.ID, String.valueOf(id), Store.NO));
  }

  public static void addFileData(FileData fileData, Document doc) {
    doc.add(new StringField(LuceneConstants.PATH, fileData.getPersistentPath(), Store.YES));
    doc.add(new TextField(LuceneConstants.CONTENT, fileData.getContent(), Store.YES));
    doc.add(new TextField(LuceneConstants.SEARCH_CONTENT, "", Store.NO));
    doc.add(new StringField(LuceneConstants.SUBMITTER, fileData.getSubmitter(), Store.YES));
  }

  public static FileData parseFileData(Document doc) {
    return FileData.builder()
      .submitter(doc.get(LuceneConstants.SUBMITTER))
      .persistentPath(doc.get(LuceneConstants.PATH))
      .content(doc.get(LuceneConstants.CONTENT))
      .metadata(MetadataMapper.parseMetadata(doc))
      .build();
  }

}
