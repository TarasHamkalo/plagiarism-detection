from sympy.physics.units import current

from token.Token import Token
from .lexer import Lexer




class TextLexer(Lexer):

  def __init__(self, reader):
    super().__init__(reader)

  # Stanza fails when word-is split by dash
  # Stanza do not ignore pucnt we should remove it
  def next_token(self, path):
    self.trim_left()
    if self.current.isalnum():
      return self.read_word()
    # start = chunk['position']
      # content = ""
      # for char in chunk['content']:
      #   if char.isspace():
      #     continue
      #
      #   if char.isalnum():
      #     content += char

  def read_word(self):
    content = ""
    start = self.position
    while self.current.isalnum():
      content += self.read_word()

    return Token(content, content, start, self.position)

  def trim_left(self):
    while self.current.isspace():
      self.consume()

  # get text chunk
  # go through it, check for punctuation
  # text while cars append on punctuation
  # return punct on numbers return numbers
  def read_till_white_space(self, chunk, file):
    while not chunk[-1].isspace():
      content = file.read(1)
      if len(content) == 0:
        break

      chunk += content
    return chunk
