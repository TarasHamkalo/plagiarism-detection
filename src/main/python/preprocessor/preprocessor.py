import stanza
import re


class Preprocessor:
  nlp = stanza.Pipeline('sk', processors='tokenize,mwt,pos,lemma')
  new_line_broken_word = re.compile(r'(\w+)-\n(\w+)')
  def tokenize(self, path):
    with open(path, 'r', encoding='utf-8') as f:
      # TODO: buffering
      content = f.read()
      re.sub(self.new_line_broken_word, r'\1\2', content)
      content