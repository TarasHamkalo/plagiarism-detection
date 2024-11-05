//package dev.taras.hamkalo.plagdet.index;
//
//import dev.taras.hamkalo.plagdet.documents.DocumentEntry;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class IndexSequence {
//
//  FileSystemStore fileSystemIndex;
//
//  List<DocumentIndex> retrieverSequence;
//
//  List<DocumentIndex> pushSequence;
//
//  public DocumentEntry pushToIndex(DocumentEntry documentEntry) {
//    // TODO: exceptions ignored ?
//    for (var index : pushSequence) {
//      documentEntry = index.pushToIndex(documentEntry);
//    }
//
//    return documentEntry;
//  }
//
//  public List<DocumentEntry> retrieveSimilar(DocumentEntry documentEntry) {
//    Map<String, DocumentEntry> similar = new HashMap<>();
//    for (var index : retrieverSequence) {
//      for (var document : index.retrieveSimilar(documentEntry)) {
//        var path = document.getPath().toString();
//        if (!similar.containsKey(path)) {
//          // TODO: move fs to other interface
//          document = fileSystemIndex.retrieveSimilar(document).get(0);
//          similar.put(path, document);
//        }
//      }
//    }
//
//    return new ArrayList<>(similar.values());
//  }
//
//}
