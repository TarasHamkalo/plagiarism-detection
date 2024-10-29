from collections import defaultdict


class Fingerprint:

  def __init__(self, hash):
    self.hash = hash
    # doc - positions
    self.occureneces = defaultdict(list())


  def add_occurenece(self, file_path, start, end):
    self.occureneces[file_path].append({'start': start, 'end': end})