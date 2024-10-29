from typing import Dict

from fingerprinting.fingerprint import Fingerprint


class FingerprintIndex:

  def __init__(self):
    # Hash to fingerprint (document -> list(occurences)
    self.index : Dict[int, Fingerprint] = {}


  def record(self, file_path : str, hash_value : int, start, end) -> None:
    if hash_value not in self.index:
      self.index[hash_value] = Fingerprint(hash_value)

    self.index[hash_value].add_occurrence(file_path, start, end)