import logging
from typing import List

import operator

from comparison.submission import Submission
from fingerprinting.fingerprint_index import FingerprintIndex
from hashing.hasher import Hasher
from preprocessing.text_token import TextToken

log = logging.getLogger(__name__)

class Winnowing:

  def __init__(self, index : FingerprintIndex, window_size = 3):
    self.index = index
    self.window_size = window_size

  def winnowing_fingerprint(self, submission : Submission) -> None:
    hasher = Hasher(self.window_size)

    submission_id = submission.id
    tokens = submission.tokens

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
      # window.append(hasher.rolling_hash(token.content))
      window.append(hasher.hash(token.content))

      token_pos.append(i)
      # if i < self.window_size - 1:
      #   continue

      min_pos = min_pos - 1

      if min_pos < 0:
        # TODO: should be rightmost
        min_pos, min_value = min(enumerate(window), key=operator.itemgetter(1))

        # TODO: recording whole window as a match gives too may false positives, use min token instead, and try kgrams...
        self.index.record(
          submission_id,
          window[min_pos],
          tokens[token_pos[min_pos]].start,
          tokens[token_pos[min_pos]].end,
          # tokens[token_pos[0]].start,
          # tokens[token_pos[-1]].end
        )
      else:
        if window[min_pos] > window[-1]:
          min_pos = len(window) - 1
          self.index.record(
            submission_id,
            window[min_pos],
            tokens[token_pos[min_pos]].start,
            tokens[token_pos[min_pos]].end,
            # tokens[token_pos[0]].start,
            # tokens[token_pos[-1]].end
          )

if __name__ == '__main__':
  index = FingerprintIndex()
  text1 = "proces pr pošle signál sigusr1 svojmu hlavnému procesu"
  text1 = "proces pr by mal poslat signal, ktory prijme dalsi proces"

  winnowing = Winnowing(index)

