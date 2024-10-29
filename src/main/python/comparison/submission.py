from typing import List

from preprocessing.text_token import TextToken

class Submission:

  def __init__(self, path: str, tokens: List[TextToken]):
    self.id = hash(path)
    self.path = path
    self.tokens = tokens
    self.length = tokens[-1].end - tokens[0].start