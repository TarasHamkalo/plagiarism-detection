package dev.taras.hamkalo.plagdet.indexing.lucene;

import dev.taras.hamkalo.plagdet.documents.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import static org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;

import java.util.Date;
import java.util.Optional;

@Slf4j
public class MetadataMapper {

  public static void addMetadata(Metadata metadata, Document doc) {
    Optional.ofNullable(metadata.getCreator()).ifPresent(creator -> {
      doc.add(new StringField(LuceneConstants.CREATOR, creator, Store.YES));
    });

    Optional.ofNullable(metadata.getCreationDate()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.CREATION_DATE, dateToString(date), Store.YES));
    });

    var totalEditTime = metadata.getTotalEditTime().toString();
    doc.add(new StringField(LuceneConstants.TOTAL_EDIT_TIME, totalEditTime, Store.YES));

    Optional.ofNullable(metadata.getModifier()).ifPresent(modifier -> {
      doc.add(new StringField(LuceneConstants.MODIFIER, modifier, Store.YES));
    });

    Optional.ofNullable(metadata.getModificationDate()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.MODIFICATION_DATE, dateToString(date), Store.YES));
    });

    Optional.ofNullable(metadata.getLastPrinted()).ifPresent(date -> {
      doc.add(new StringField(LuceneConstants.LAST_PRINTED, dateToString(date), Store.YES));
    });

    var revisionsNumber = metadata.getRevisionsNumber().toString();
    doc.add(new StringField(LuceneConstants.REVISIONS_NUMBER, revisionsNumber, Store.YES));
  }

  public static Metadata parseMetadata(Document doc) {
    Date creationDate = parseDate(doc.get(LuceneConstants.CREATION_DATE));
    Date modificationDate = parseDate(doc.get(LuceneConstants.MODIFICATION_DATE));
    Date lastPrinted = parseDate(doc.get(LuceneConstants.LAST_PRINTED));

    return Metadata.builder()
      .creator(doc.get(LuceneConstants.CREATOR))
      .creationDate(creationDate)
      .totalEditTime(parseLong(doc.get(LuceneConstants.TOTAL_EDIT_TIME), 0L))
      .revisionsNumber(parseInt(doc.get(LuceneConstants.REVISIONS_NUMBER), 0))
      .modificationDate(modificationDate)
      .lastPrinted(lastPrinted)
      .modifier(doc.get(LuceneConstants.MODIFIER))
      .build();
  }


  private static String dateToString(Date date) {
    return DateTools.dateToString(date, DateTools.Resolution.SECOND);
  }

  private static long parseLong(String value, long defaultValue) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static int parseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static Date parseDate(String dateString) {
    if (dateString == null) {
      return null;
    }

    try {
      return DateTools.stringToDate(dateString);
    } catch (Exception e) {
      log.warn("Invalid date format: {}", dateString, e);
      return null;
    }
  }

}
