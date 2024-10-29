from collections import defaultdict


class Fingerprint:

  def __init__(self, hash : int):
    self.hash = hash
    self.occurrences = defaultdict(list)


  def add_occurrence(self, file_path, start, end):
    self.occurrences[file_path].append({'start': start, 'end': end})