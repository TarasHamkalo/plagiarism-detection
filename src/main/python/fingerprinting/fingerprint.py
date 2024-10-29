from collections import defaultdict
from typing import TypedDict

class Span(TypedDict):
  start: int
  end: int
  length: int

class Fingerprint:

  def __init__(self, hash : int):
    self.hash = hash
    self.occurrences = defaultdict(list)

  def add_occurrence(self, submission_id, start, end):
    self.occurrences[submission_id].append({'start': start, 'end': end, 'length': end - start})