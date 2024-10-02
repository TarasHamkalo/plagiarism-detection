#!/usr/bin/bash

declare -r INPUT_DIR=$1
declare -r OUTPUT_DIR='processed'

declare -r META_DIR=$OUTPUT_DIR/meta
declare -r TEXT_DIR=$OUTPUT_DIR/text
declare -r DOCX_DIR=$OUTPUT_DIR/docx

if [ -z $INPUT_DIR ]
then
  echo "./script \$INPUT_DIR"
  exit 1
fi

mkdir -p $DOCX_DIR 
mkdir -p $META_DIR
mkdir -p $TEXT_DIR

cp "$INPUT_DIR"/*.docx "$DOCX_DIR" 2>/dev/null

libreoffice --headless --convert-to 'docx' $INPUT_DIR/*.doc --outdir $DOCX_DIR 

exiftool $DOCX_DIR -XMP:ALL -XML:ALL -j -n -w $META_DIR/%f.json

for document in $(ls $DOCX_DIR | egrep '^[^\.]+' -o)
do
  pandoc "$DOCX_DIR/${document}.docx" -t plain |
    tr -s '[[:punct:][:space:]]' ' ' |
    tr '[:upper:]' '[:lower:]' |
    iconv -f utf8 -t ascii//TRANSLIT > "$TEXT_DIR/${document}.txt" 
done
