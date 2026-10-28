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

if ls "$INPUT_DIR"/*.doc >/dev/null 2>&1; then
  libreoffice --headless --convert-to 'docx' $INPUT_DIR/*.doc --outdir $DOCX_DIR 
fi

pushd .
  cd $DOCX_DIR
  documents=$(ls *.docx | egrep '^[^\.]+' -o)
popd

for document in $documents; do
  # remove duplicates
  doc_hash=$(cksum "${DOCX_DIR}/${document}.docx" | awk '{print $1}')
  if [ -e "${DOCX_DIR}/${doc_hash}.docx" ]; then
    echo duplicate $document=$doc_hash
    continue
  fi

  # create text
  pandoc "${DOCX_DIR}/${document}.docx" -t plain -o "${TEXT_DIR}/${doc_hash}.txt" 2> err.pandoc

  exiftool "${DOCX_DIR}/${document}.docx" -XMP:ALL -XML:ALL -j -n -w "${META_DIR}/%f.json" 2> err.exiftool
  if [ ! -e "${META_DIR}/${document}.json" ]
  then
    failed_documents="$DOCX_DIR/${doc_hash}.docx $failed_documents" 
  else
    mv "${META_DIR}/${document}.json" "${META_DIR}/${doc_hash}.json"
  fi 

  mv "${DOCX_DIR}/${document}.docx" "${DOCX_DIR}/${doc_hash}.docx"

done

pushd .
  cd $INPUT_DIR
  documents=$(ls *.docx | egrep '^[^\.]+' -o)
popd

for document in $documents; do
  echo $document is document
  
  doc_hash=$(cksum "$INPUT_DIR/${document}.docx" | awk '{print $1}')
  if [ -e "$DOCX_DIR/${doc_hash}.docx" ]; then
    echo duplicate $document=$doc_hash
    continue
  fi

  echo document name=hash: $document=$doc_hash

  cp "$INPUT_DIR/${document}.docx" "$DOCX_DIR/${doc_hash}.docx"

  pandoc "$DOCX_DIR/${document}.docx" -t plain -o "$TEXT_DIR/${doc_hash}.txt" 2> err.pandoc

  exiftool "$INPUT_DIR/${document}.docx" -XMP:ALL -XML:ALL -j -n -w "${META_DIR}/%f.json" 2> err.exiftool
  if [ ! -e "${META_DIR}/${document}.json" ]
  then
    failed_documents="$DOCX_DIR/${doc_hash}.docx $failed_documents" 
  else
    mv "$META_DIR/${document}.json" "$META_DIR/${doc_hash}.json"
  fi 

done
echo Failed documents list: $failed_documents
#
# for document in $(ls $DOCX_DIR | egrep '^[^\.]+' -o)
# do
#   echo $(ls -l "$DOCX_DIR/${document}.docx")
#   doc_hash=$(cksum "$DOCX_DIR/${document}.docx" | awk '{print $1}')
#
#   pandoc "$DOCX_DIR/${document}.docx" -t plain -o "$TEXT_DIR/${doc_hash}.txt" &> /dev/null
#   exiftool "$DOCX_DIR/${document}.docx" -XMP:ALL -XML:ALL -j -n -w "${META_DIR}/%f.json" &> /dev/null
#
#   if [ ! -e "${META_DIR}/${document}.json" ]
#   then
#     failed_documents="$DOCX_DIR/${doc_hash}.docx $failed_documents"0 
#   else
#     mv "$META_DIR/${document}.json" "$META_DIR/${doc_hash}.json"
#   fi 
#
#   mv "$DOCX_DIR/${document}.docx" "$DOCX_DIR/${doc_hash}.docx"
#
#     # tr -s '[[:punct:][:space:]]' ' ' |
#     # tr '[:upper:]' '[:lower:]' |
#     # iconv -f utf8 -t ascii//TRANSLIT > "$TEXT_DIR/${document}.txt" 
# done
#
#
