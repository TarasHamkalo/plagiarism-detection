import logging
from typing import List

import operator

from fingerprinting.fingerprint_index import FingerprintIndex
from preprocessing.text_token import TextToken

log = logging.getLogger(__name__)

class Winnowing:

  def __init__(self, index : FingerprintIndex, window_size = 3):
    self.index = index
    self.window_size = window_size

  def winnowing_fingerprint(self, file_path : str, tokens : List[TextToken]) -> None:
    if len(tokens) < self.window_size:
      raise Exception("File is too small for give window size")

    window = [float('inf')] * self.window_size
    token_pos = [-1] * self.window_size

    min_pos = 0

    for i, token in enumerate(tokens): # type: int, TextToken
      # TODO: replace with circular buffer
      window = window[1:]
      token_pos = token_pos[1:]

      # TODO: use rolling hash, to enforce same order for matching
      window.append(self.hash(token.content))
      token_pos.append(i)
      log.debug(window)
      if i < self.window_size - 1:
        continue

      min_pos = min_pos - 1

      if min_pos < 0:
        min_pos, min_value = min(enumerate(window), key=operator.itemgetter(1))
        self.index.record(
          file_path,
          window[min_pos],
          tokens[token_pos[0]].start,
          tokens[token_pos[-1]].end
        )
      else:
        if window[min_pos] > window[-1]:
          min_pos = len(window) - 1
          self.index.record(
            file_path,
            window[min_pos],
            tokens[token_pos[0]].start,
            tokens[token_pos[-1]].end
          )

  @staticmethod
  def hash(s : str):
    mod = 33554393
    base = 747287

    hash_value = 0
    for char in s:
      hash_value = ((hash_value + ord(char)) * base) % mod

    return hash_value


if __name__ == '__main__':
  print(Winnowing.hash('abc'))
  print(Winnowing.hash('a'))

  print([Winnowing.hash(s) for s in ['a', 'b', 'c']])
  print([Winnowing.hash(s) for s in ['c', 'b', 'a']])
  index = FingerprintIndex()
  text1 = "proces pr pošle signál sigusr1 svojmu hlavnému procesu"
  text1 = "proces pr by mal poslat signal, ktory prijme dalsi proces"

  winnowing = Winnowing(index)

