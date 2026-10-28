#!/usr/bin/env bash

set -euo pipefail

MODEL_DIR="${MODEL_DIR:-models-tmp}"
mkdir -p "$MODEL_DIR"

MORPH_URL="https://lindat.mff.cuni.cz/repository/server/api/core/bitstreams/handle/11234/1-3278/slovak-morfflex-pdt-170914.zip"
VECTORS_URL="https://www.juls.savba.sk/data/sem%C3%A4/sk.poslemma.prim-10.0-public-saneword2vec.txt.xz"

curl -fL "$MORPH_URL" \
    -o "$MODEL_DIR/morfflex.zip"

unzip -o "$MODEL_DIR/morfflex.zip" \
    -d "$MODEL_DIR/morfflex-tmp"

find "$MODEL_DIR/morfflex-tmp" \
    -type f \
    -name 'slovak-morfflex-pdt-170914-pos_only.tagger' \
    -exec cp {} "$MODEL_DIR/slovak-morfflex-pdt-170914-pos_only.tagger" \;

test -s "$MODEL_DIR/slovak-morfflex-pdt-170914-pos_only.tagger"

curl -fL "$VECTORS_URL" \
    -o "$MODEL_DIR/vectors.txt.xz"

xz -dc "$MODEL_DIR/vectors.txt.xz" \
    > "$MODEL_DIR/vectors.txt"

python3 scripts/convert-vectors.py \
    "$MODEL_DIR/vectors.txt" \
    "$MODEL_DIR/sk.poslemma.prim-10.0-public-saneword2vec.bin"

echo "Models prepared in $MODEL_DIR"
