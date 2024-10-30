from typing import TypedDict, List

from comparison.submission import Submission
from fingerprinting.fingerprint import Fingerprint, Span
from fingerprinting.fingerprint_index import FingerprintIndex


class CommonFragment(TypedDict):
  span_a: Span
  span_b: Span

class SubmissionPair:

  def __init__(self, index : FingerprintIndex, submission_a : Submission, submission_b : Submission):
    self.submission_a = submission_a
    self.submission_b = submission_b
    self.fingerprints_a = index.submission_to_fingerprint[self.submission_a.id]
    self.fingerprints_b = index.submission_to_fingerprint[self.submission_b.id]
    self.common = self.fingerprints_a.intersection(self.fingerprints_b)
    self.submission_a_coverage: int = 0
    self.submission_b_coverage: int = 0
    self.total_overlap: int = 0

    # self.common_fragments: List[CommonFragment] =
    self.build_fragments()

  def build_fragments(self):
    # self.common_fragments: List[CommonFragment] = []
    for fingerprint in self.common: # type: Fingerprint
      for occurrence_a in fingerprint.occurrences[self.submission_a.id]:
        self.submission_a_coverage += occurrence_a['length']
      for occurrence_b in fingerprint.occurrences[self.submission_b.id]:
        self.submission_b_coverage += occurrence_b['length']

    # go through common fingerprints, and pair each occurrence from submission a to b
      # self.common_fragments.append({
      #   'span_a': occurrence_a,
      #   'span_b': occurrence_b,
      # })

    self.total_overlap = self.submission_a_coverage + self.submission_b_coverage
    # <0, 1>: <1, 1>, <1, 2>

  def avg_similarity(self) -> float:
    return self.total_overlap / (self.submission_a.length + self.submission_b.length)