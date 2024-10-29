from collections import defaultdict
from typing import Dict, List, Set

from comparison.submission import Submission
from fingerprinting.fingerprint import Fingerprint


class FingerprintIndex:

  def __init__(self):
    # Hash to fingerprint (document -> list(occurences)
    self.hash_to_fingerprint: Dict[int, Fingerprint] = {}

    # Submission id to fingerprint (document -> list(occurences)
    self.submission_to_fingerprint: Dict[int, Set[Fingerprint]] = defaultdict(set)

  def record(self, submission_id, hash_value: int, start, end) -> None:
    if hash_value not in self.hash_to_fingerprint:
      self.hash_to_fingerprint[hash_value] = Fingerprint(hash_value)

    f = self.hash_to_fingerprint[hash_value]
    f.add_occurrence(submission_id, start, end)

    if f not in self.submission_to_fingerprint[submission_id]:
      self.submission_to_fingerprint[submission_id].add(f)