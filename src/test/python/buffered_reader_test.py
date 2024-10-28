from reader.buffered_reader import BufferedReader

PATH = "/home/taras-hamkalo/repositories/plagiarism-detection/src/test/resources/13.txt"
PATH = "/home/taras-hamkalo/repositories/plagiarism-detection/src/test/resources/short.txt"

reader = BufferedReader(7)

for chunk in reader.next(PATH):
  print(chunk)