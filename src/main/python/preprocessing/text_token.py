class TextToken:

  def __init__(self, content, type, start, end):
    self.content = content
    self.type = type
    self.start = start
    self.end = end

  @staticmethod
  def from_stanza_word(stanza_word):
    """
    :param stanza_word:
    :return: None if content of word is zero len and Token otherwise
    """
    if len(stanza_word.text) == 0:
      return None

    content = stanza_word.lemma
    if content is None:
      content = stanza_word.text

    if stanza_word.upos == 'PUNCT' or stanza_word.upos == 'NUM':
      type = stanza_word.upos
    else:
      type = content

    return TextToken(content, type, stanza_word.start_char, stanza_word.end_char)

  def ispunct(self):
    return self.type == 'PUNCT'