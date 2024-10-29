import sys
import operator

from fingerprint_index import FingerprintIndex

class Winnowing:
  window_size = 3

  def __init__(self, index):
    self.index = index

  def winnowing_fingerprint(self, file_path, tokens):
    """
    For each k-gram
    :param file_path:
    :param tokens:
    :return:
    """
    if tokens < self.window_size:
      raise Exception("File is too small for give window size")

    window = [float('inf')] * self.window_size
    token_pos = [-1] * self.window_size

    wi = 0 # buffer write position
    ri = 1 # buffer read position
    min_index = 0 # index of min value

    for i, token in enumerate(tokens):
      wi = (wi + 1) % self.window_size
      if wi == ri:
        ri = (ri + 1) % self.window_size

      # TODO: use rolling hash, to enforce same order for matching
      window[wi] = self.hash(token)
      token_pos[wi] = i

      if wi == min_index:
        # find new min
        min_index, min_value = min(enumerate(window), key=operator.itemgetter(1))
        min_token_pos = token_pos[min_index]

        # TODO: record start of the window (as we don't enforce order in a window)
        # TODO: take tokens from those positions and take the last_char
        # ri = (wi + 1) % self.window_size
        window_start = tokens[token_pos[ri]].start_char
        window_end = tokens[token_pos[wi]].end_char

        self.index.record(
          file_path,
          window[min_index],
          window_start,
          window_end,
        )

      elif window[wi] < window[min_index]:
        min_index = wi
        window_start = tokens[token_pos[ri]].start_char
        window_end = tokens[token_pos[wi]].end_char
        self.index.record(
          file_path,
          window[min_index],
          window_start,
          window_end,
        )
    # fill window with hashes
    # pick the least hash
    # record it

  @staticmethod
  def hash(s):
    mod = 33554393
    base = 747287

    hash_value = 0
    for char in s:
      hash_value = ((hash_value + ord(char)) * base) % mod

    return hash_value

  # @staticmethod
  # def rolling_hash(, )

if __name__ == '__main__':
  print(Winnowing.hash('abc'))
  print(Winnowing.hash('a'))

  print([Winnowing.hash(s) for s in ['a', 'b', 'c']])
  print([Winnowing.hash(s) for s in ['c', 'b', 'a']])
  index = FingerprintIndex()
  text1 = "proces pr pošle signál sigusr1 svojmu hlavnému procesu"
  text1 = "proces serv1 vytvorí tcp server"

  winnowing = Winnowing(index)


# print(ord('č'))
# print(ord('c'))
