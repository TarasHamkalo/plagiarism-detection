import sys
from gensim.models import KeyedVectors

source, target = sys.argv[1:3]

vectors = KeyedVectors.load_word2vec_format(
    source,
    binary=False,
)

vectors.save_word2vec_format(
    target,
    binary=True,
)

print(f"Saved {len(vectors):,} vectors to {target}")