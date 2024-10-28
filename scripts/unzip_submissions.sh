#!/usr/bin/bash
declare -r INPUT_DIR=$1
declare -r OUTPUT_DIR='unzipped'
declare -r DOCS_DIR=$OUTPUT_DIR/docs

if [ -z $INPUT_DIR ]
then
  echo "./script \$INPUT_DIR"
  exit 1
fi

pushd . 

cd $INPUT_DIR

mkdir -p $OUTPUT_DIR
mkdir -p $DOCS_DIR 

zips=$(ls *.zip | egrep '^[^\.]+' -o)

for zipp in $zips; do
  echo $zipp is processed 

  unzip -d "$OUTPUT_DIR/$zipp" "${zipp}.zip"

  doc_path=$(find "$OUTPUT_DIR/$zipp" -type f -name "*.docx" -print -quit)
  echo "$doc_path"
  if [ -z "$doc_path" ]; then
    echo docx file was not found in $zipp
    doc_path=$(find "$OUTPUT_DIR/$zipp" -type f -name "*.doc" -print -quit)
  fi 

  if [ -z "$doc_path" ]; then
    echo doc file was not found in $zipp
    continue
  fi 
  
  extension="${doc_path##*.}"
  echo "$doc_path" ext is $extension
  doc_hash=$(cksum "$doc_path" | awk '{print $1}')
  echo "$doc_path found in $zipp (when hashed is $doc_hash)"

  echo copying "$doc_path" to $DOCS_DIR
  doc_new_path="${DOCS_DIR}/${doc_hash}.${extension}"
  if [ -e "doc_new_path" ]; then
    echo $doc_new_path already exists, duplicate found
    continue
  fi

  cp "$doc_path" "$doc_new_path"
done

popd
