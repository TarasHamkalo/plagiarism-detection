package dev.taras.hamkalo.plagdet.preprocessing.transformation.stemmers;


/*
  Sources:
    http://members.unine.ch/jacques.savoy/clef/CzechStemmerAgressive.txt
    https://github.com/business-semantics/stemmer-sk/blob/master/src/main/java/sk/essentialdata/nlp/stemmer/sk/SlovakStemmer.java
    https://github.com/mrshu/stemm-sk/
 */
import java.util.Set;

public class Stemmer {

  public static String stem(String word, boolean aggressive) {
    String s = word.toLowerCase();
    s = removeCase(s);
    s = removePossessives(s);

    if (aggressive) {
      s = removeComparative(s);
      s = removeDiminutive(s);
      s = removeAugmentative(s);
      s = removeDerivational(s);
    }

    return s;
  }

  private static String removeCase(String word) {
    if (word.endsWith("atoch") && word.length() > 7) {
      return word.substring(0, word.length() - 5);
    }

    if (word.endsWith("aťom") && word.length() > 6) {
      return palatalise(word.substring(0, word.length() - 3));
    }

    if (word.length() > 5) {
      var suffixSet1 = Set.of(
        "och", "ich", "ích", "ého", "ami", "emi", "ému",
        "ete", "eti", "iho", "ího", "ími", "imu", "aťa"
      );

      if (suffixSet1.contains(word.substring(word.length() - 3))) {
        return palatalise(word.substring(0, word.length() - 2));
      }

      var suffixSet2 = Set.of(
        "ách", "ata", "aty", "ých", "ami",
        "ové", "ovi", "ými"
      );

      if (suffixSet2.contains(word.substring(word.length() - 3))) {
        return palatalise(word.substring(0, word.length() - 3));
      }
    }


    if (word.length() > 4) {
      if (word.endsWith("om")) {
        return palatalise(word.substring(0, word.length() - 1));
      }

      if (Set.of("es", "ém", "ím").contains(word.substring(word.length() - 2))) {
        return palatalise(word.substring(0, word.length() - 2));
      }

      var suffixSet3 = Set.of(
        "úm", "at", "ať", "ám", "os", "us", "ým", "mi", "ou", "ej"
      );
      if (suffixSet3.contains(word.substring(word.length() - 2))) {
        return word.substring(0, word.length() - 2);
      }
    }

    if (word.length() > 3) {
      if ("eií".contains(word.substring(word.length() - 1))) {
        return palatalise(word);
      }

      if ("úyaoáéý".contains(word.substring(word.length() - 1))) {
        return word.substring(0, word.length() - 1);
      }

    }

    return word;
  }

  private static String removePossessives(String word) {
    if (word.length() > 5) {
      if (word.endsWith("ov")) {
        return word.substring(0, word.length() - 2);
      }

      if (word.endsWith("in")) {
        return palatalise(word.substring(0, word.length() - 1));
      }
    }

    return word;
  }

  private static String removeComparative(String word) {
    if (word.length() > 5) {
      if (Set.of("ejš", "ějš").contains(word.substring(word.length() - 3))) {
        return palatalise(word.substring(0, word.length() - 2));
      }
    }

    return word;
  }

  private static String removeDiminutive(String word) {

    if (word.length() > 7 && word.endsWith("oušok")) {
      return word.substring(0, word.length() - 5);
    }

    if (word.length() > 6) {
      var last4chars = word.substring(word.length() - 4);
      var suffixSet1 = Set.of(
        "ečok", "éčok", "ičok", "íčok", "enok", "énok",
        "inok", "ínok"
      );

      if (suffixSet1.contains(last4chars)) {
        return palatalise(word.substring(0, word.length() - 3));
      }

      var suffixSet2 = Set.of(
        "áčok", "ačok", "očok", "učok", "anok", "onok",
        "unok", "ánok"
      );

      if (suffixSet2.contains(last4chars)) {
        return palatalise(word.substring(0, word.length() - 4));
      }
    }

    if (word.length() > 5) {
      var last3chars = word.substring(word.length() - 3);
      var suffixSet1 = Set.of(
        "ečk", "éčk", "ičk", "íčk", "enk", "énk",
        "ink", "ínk"
      );

      if (suffixSet1.contains(last3chars)) {
        return palatalise(word.substring(0, word.length() - 3));
      }

      var suffixSet2 = Set.of(
        "áčk", "ačk", "očk", "učk", "ank", "onk",
        "unk", "átk", "ánk", "ušk"
      );

      if (suffixSet2.contains(last3chars)) {
        return word.substring(0, word.length() - 3);
      }
    }

    if (word.length() > 4) {
      var last2chars = word.substring(word.length() - 2); // TODO: there was bug 3
      if (Set.of("ek", "ék", "ík", "ik").contains(last2chars)) {
        return palatalise(word.substring(0, word.length() - 1));
      }

      if (Set.of("ák", "ak", "ok", "uk").contains(last2chars)) {
        return word.substring(0, word.length() - 1);
      }
    }

    if (word.length() > 3 && word.endsWith("k")) {
      return word.substring(0, word.length() - 1);
    }

    return word;
  }

  private static String removeAugmentative(String word) {
    if (word.length() > 6 && word.endsWith("ajzn")) {
      return word.substring(0, word.length() - 4);
    }

    if (word.length() > 5 &&
        Set.of("izn", "isk").contains(word.substring(word.length() - 3))) {

        return palatalise(word.substring(0, word.length() - 2));
    }

    if (word.length() > 4 && word.endsWith("ák")) {
      return word.substring(0, word.length() - 2);
    }

    return word;
  }

  private static String removeDerivational(String word) {
    if (word.length() > 8 && word.endsWith("obinec")) {
      return word.substring(0, word.length() - 6);
    }

    if (word.length() > 7) {
      if (word.endsWith("ionár")) {
        return palatalise(word.substring(0, word.length() - 4));
      }
      var suffixSet1 = Set.of("ovisk", "ovstv", "ovišt", "ovník");
      if (suffixSet1.contains(word.substring(word.length() - 5))) {
        return word.substring(0, word.length() - 5);
      }
    }

    if (word.length() > 6) {
      var suffixSet1 = Set.of(
        "ások", "nosť", "teln", "ovec", "ovík",
        "ovtv", "ovin", "štin"
      );

      if (suffixSet1.contains(word.substring(word.length() - 4))) {
        return word.substring(0, word.length() - 4);
      }

      if (Set.of("enic", "inec", "itel").contains(word.substring(word.length() - 4))) {
        return palatalise(word.substring(0, word.length() - 3));
      }
    }

    if (word.length() > 5) {
      if (word.endsWith("árn")) {
        return word.substring(0, word.length() - 3);
      }

      var suffixSet1 = Set.of(
        "enk", "ián", "ist", "isk", "išt", "itb", "írn"
      );

      if (suffixSet1.contains(word.substring(word.length() - 3))) {
        return palatalise(word.substring(0, word.length() - 2));
      }

      var suffixSet2 = Set.of(
        "och", "ost", "ovn", "oun", "out", "ouš", "ušk",
        "kyn", "čan", "kář", "néř", "ník", "ctv", "stv"
      );

      if (suffixSet2.contains(word.substring(word.length() - 3))) {
        return word.substring(0, word.length() - 3);
      }
    }

    if (word.length() > 4) {
      var suffixSet1 = Set.of("áč", "ač", "án", "an", "ár", "ar", "ás", "as");
      if (suffixSet1.contains(word.substring(word.length() - 2))) {
        return word.substring(0, word.length() - 2);
      }

      var suffixSet2  = Set.of(
        "ec", "en", "ér", "ír", "ic", "in", "ín",
        "it", "iv", "iť"
      );

      if (suffixSet2.contains(word.substring(word.length() - 2))) {
        return palatalise(word.substring(0, word.length() - 1));
      }

      var suffixSet3 = Set.of(
        "ob", "ot", "ov", "oň", "ul", "yn", "čk", "čn",
        "dl", "nk", "tv", "tk", "vk"
      );

      if (suffixSet3.contains(word.substring(word.length() - 2))) {
        return word.substring(0, word.length() - 2);
      }
    }

    if (word.length() > 3 && "cčklnt".contains(word.substring(word.length() - 1))) {
      return word.substring(0, word.length() - 1);
    }

    return word;
  }

  private static String palatalise(String word) {
    if (Set.of("ci", "ce", "či", "če").contains(word.substring(word.length() - 2))) {
      return word.substring(0, word.length() - 2) + "k";
    }

    if (Set.of("zi", "ze", "ži", "že").contains(word.substring(word.length() - 2))) {
      return word.substring(0, word.length() - 2) + "h";
    }

    if (Set.of("čte", "čti", "čtí").contains(word.substring(word.length() - 3))) {
      return word.substring(0, word.length() - 3) + "ck";
    }
    if (Set.of("šte", "šti", "ští").contains(word.substring(word.length() - 3))) {
      return word.substring(0, word.length() - 3) + "sk";
    }

    return word.substring(0, word.length() - 1);
  }

//  public static void main(String[] args) {
////    var words = Set.of("Vytvorit", "Vytvarat", "Vytvorenie",  "Vytvorim", "Vytvoria");
////    var words = Set.of("Zdrojov", "Prespracovanie", "Prespracovavanie");
//    var words = Set.of("Zdrojov", "dodané", "dodaní", "svojmu", "muz", "muzsky");
//    for (var word : words) {
//      System.out.println(word + "==" + stem(word, true));
//    }
//  }
}
