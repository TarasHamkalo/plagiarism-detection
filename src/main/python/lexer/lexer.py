from io import FileIO, TextIOWrapper

from reader.buffered_reader import BufferedReader


class Lexer:

  def __init__(self, reader: BufferedReader):
    self._reader = reader
    self.current = 0
    self.position = 0

  def next_token(self, path):
    """
    :return: returns token for given file, and position
    """
    raise NotImplemented('Abstract method')

  def consume(self):
    self.current = self._reader.consume()
    self.position += 1