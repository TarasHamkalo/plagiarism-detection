import logging
import logging.config
import pathlib

import stanza
import yaml

from fingerprinting.fingerprint_index import FingerprintIndex
from fingerprinting.winnowing import Winnowing
from preprocessing.Preprocessor import Preprocessor

# stanza.download('sk')

logger = logging.getLogger('MAIN')

def setup_logging():
  config_file = pathlib.Path("resources/logging.yaml")
  with open(config_file, 'r') as f:
    config = yaml.safe_load(f)

  logging.config.dictConfig(config)


def main():
  # nlp = stanza.Pipeline('sk', processors='tokenize,mwt,pos,lemma')
  nlp = stanza.Pipeline('sk', processors='tokenize,lemma')

  # DOC1_PATH = "/home/taras-hamkalo/repositories/plagiarism-detection/src/test/resources/3.txt"
  # DOC2_PATH = "/home/taras-hamkalo/repositories/plagiarism-detection/src/test/resources/4.txt"

  # with open(DOC1_PATH, 'r') as f:
  #   doc1 = nlp(f.read())
  #
  # with open(DOC2_PATH, 'r') as f:
  #   doc2 = nlp(f.read())
  #
  # doc_tokens_1 = Preprocessor.format_tokens_from_stanza_doc(doc1)
  # doc_tokens_2 = Preprocessor.format_tokens_from_stanza_doc(doc2)

  index = FingerprintIndex()

  text1 = "proces pr pošle signál sigusr1 svojmu hlavnému procesu"
  text2 = "proces pr blabla proces pr by mal poslat signal sigusr1, ktory prijme dalsi proces"

  # text2 = "proces serv1 vytvorí tcp server"
  # text2 = "proces pr by mal poslat signal, ktory prijme dalsi proces"
  tokens_text_1 = Preprocessor.format_tokens_from_stanza_doc(nlp(text1))
  tokens_text_2 = Preprocessor.format_tokens_from_stanza_doc(nlp(text2))
  winnowing = Winnowing(index, 2)

  winnowing.winnowing_fingerprint("text1", tokens_text_1)
  winnowing.winnowing_fingerprint("text2", tokens_text_2)
  print(index)

if __name__ == "__main__":
  setup_logging()
  main()


