import logging
import logging.config
import pathlib

import stanza
import yaml

from comparison.submission import Submission
from comparison.submission_pair import CommonFragment
from comparison.submission_pair import SubmissionPair
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

def print_fragments(pair: SubmissionPair, text1, text2):
  for i, fragment in enumerate(pair.common_fragments): # type: CommonFragment
    print(f"{i}--")

    print("Match in text1: {m1}\nMatch in text2: {m2}".format(
      m1=text1[fragment['span_a']['start']:fragment['span_a']['end']],
      m2=text2[fragment['span_b']['start']:fragment['span_b']['end']]
    ))


def main():
  # nlp = stanza.Pipeline('sk', processors='tokenize,mwt,pos,lemma')
  nlp = stanza.Pipeline('sk', processors='tokenize,mwt,pos,lemma')
  # nlp = stanza.Pipeline('sk', processors='tokenize,lemma')

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
  doc = nlp(text2)
  tokens_text_2 = Preprocessor.format_tokens_from_stanza_doc(doc)

  submission_a = Submission("text1", tokens_text_1)
  submission_b = Submission("text2", tokens_text_2)

  winnowing = Winnowing(index, 2)
  # w + k - 1 =
  winnowing.winnowing_fingerprint(submission_a)
  winnowing.winnowing_fingerprint(submission_b)

  pair = SubmissionPair(index, submission_a, submission_b)
  print_fragments(pair, text1, text2)
  print(pair.avg_similarity())

if __name__ == "__main__":
  setup_logging()
  main()


# 0--
# Match in text1: proces pr
# Match in text2: proces pr
# 1--
# Match in text1: proces pr
# Match in text2: proces pr
# 2--
# Match in text1: sigusr1 svojmu
# Match in text2: sigusr1,
# 0.3602941176470588
