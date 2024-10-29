from collections import defaultdict
from .fingerprint import Fingerprint

class FingerprintIndex:

  def __init__(self):
    # Hash to fingerprint (document -> list(occurences)
    self.index = {}


  def record(self, file_path, hash, start, end):
    if hash not in self.index:
      self.index[hash] = Fingerprint(hash)

    self.index[hash].add_occurenece(file_path, start, end)