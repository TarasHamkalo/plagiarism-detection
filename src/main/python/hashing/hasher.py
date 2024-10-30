
class Hasher:

  def __init__(self, window_size):
    self.memory = [0] * window_size
    self.window_size = window_size
    self.hash_value = 0

  def rolling_hash(self, s: str):
    s_hash = self.hash(s)
    base = 31
    mod = 2147483647
    # compute hash based on memory state and write it to the end
    self.hash_value = ((self.hash_value - pow(base, self.window_size - 1) * self.memory[0]) + s_hash)
    self.hash_value = (self.hash_value * base) % mod
    self.memory = self.memory[1:]
    self.memory.append(s_hash)

    return self.hash_value

  @staticmethod
  def hash(s: str) -> int:
    mod = 33554393
    base = 747287

    hash_value = 0
    for char in s:
      hash_value = ((hash_value + ord(char)) * base) % mod

    return hash_value


if __name__ == '__main__':
  hasher = Hasher(3)
  h1 = [hasher.rolling_hash('abc'), hasher.rolling_hash('bca'), hasher.rolling_hash('cab')]
  hasher = Hasher(3)
  h2 = [hasher.rolling_hash('bca'), hasher.rolling_hash('abc'), hasher.rolling_hash('cab')]

  print(h1)
  print(h2)
