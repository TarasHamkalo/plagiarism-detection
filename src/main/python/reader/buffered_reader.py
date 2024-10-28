class BufferedReader:

  def __init__(self, buf_size, path):
    self.buf_size = buf_size
    self.path = path

  def consume(self) -> str:
    """
    :param path: file path to be read
    :return: text aligned to contain only whole words
    """
    with open(self.path, "r", self.buf_size) as file:
      while chunk := file.read(self.buf_size):
        for char in chunk:
          yield char
    #
    #     # TODO: probably exist some easier way
    #     chunk = self.read_till_white_space(chunk, file)
    #     # TODO find something appropriate
    #     # handle 'some-\nthing'
    #     if chunk[-1] == '-':
    #       chunk = self.read_till_white_space(chunk[:-1], file)
    #
    #     yield {'text': chunk, 'position': position}
    #     position += len(chunk)

