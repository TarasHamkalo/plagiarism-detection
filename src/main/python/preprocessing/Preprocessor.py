from preprocessing.text_token import TextToken


class Preprocessor:

  @staticmethod
  def format_tokens_from_stanza_doc(doc, skip_punctuation=True):
    tokens = []
    for sentence in doc.sentences:
      for stanza_token in sentence.tokens:
        if len(stanza_token.words) > 1:
          print(stanza_token.words)

        for word in stanza_token.words:
          token = TextToken.from_stanza_word(word)
          if token is None or (token.ispunct() and skip_punctuation):
            continue

          tokens.append(token)

    return tokens