#!/usr/bin/bash

# Program expects a directory containing zipped files
# And then does:
  # read a zip and find all doc and docx -- skip pdf
  # create a dir containing doc and docx but prefixed with their zip name 
  # create a dir containing text only 

declare -r INPUT_DIR=$1
declare -r OUTPUT_DIR=$2
# declare -r OUTPUT_DIR='processed'

declare -r TEXT_DIR=$OUTPUT_DIR/text
declare -r WORD_DIR=$OUTPUT_DIR/docx
declare -r TEMP_DIR=/tmp

if [ -z $INPUT_DIR || -z $OUTPUT_DIR ]
then
  echo "./script \$INPUT_DIR \$OUTPUT_DIR"
  exit 1
fi

mkdir -p $TEXT_DIR
mkdir -p $WORD_DIR

# combine zipname and basename and copy to WORD_DIR
function copy_docx_doc() {
  echo "$unzipped_name"
  if [ -e "$unzipped_name" ]; then
    echo "exists"
  else
    echo "don't exist"
  fi

  document_paths=$(find "$unzipped_name" -type f \( -name "*.docx" -o -name "*.doc" \))
  while IFS= read -r document_path; do
    echo "$document_path"

    document_basename="${document_path%%.*}"
    document_basename="${document_basename##*/}"

    # echo "Base: $document_basename<<<"
    document_ext="${document_path##*.}"
    # echo "Ext: $document_ext<<<"

    # echo "$WORD_DIR/$zip_basename-$document_basename.$document_ext"
    cp "$document_path" "$WORD_DIR/$zip_basename-$document_basename.$document_ext"
  done <<< "$document_paths"
}

for zip_path in "$INPUT_DIR/"*.zip; do
  # echo "$zip_path"
  zip_name="${zip_path##*/}" # selects longest pattern from start
  # echo "$zip_name"
  zip_basename="${zip_name%.zip}" # selects shortest pattern from end
  # echo "$basename"
  unzipped_name="$TEMP_DIR/$zip_basename"
  unzip -o -d "$unzipped_name" "$zip_path" 1> /dev/null 2> err.unzip
  copy_docx_doc
done

echo "Done copying"
echo "Calling libre to convert to text"
libreoffice --headless --convert-to txt:"Text (encoded):UTF8" --outdir "$TEXT_DIR" "$WORD_DIR"/*
# for document in "$WORD_DIR"/*; do
#   echo "Document for libre office: $document"
#   libreoffice --headless --convert-to txt:"Text (encoded):UTF8" --outdir "$TEXT_DIR" "$document"
# done
  
# for document in "$WORD_DIR/*"; do 
#   echo $document
# done

# if ls "$INPUT_DIR"/*.doc >/dev/null 2>&1; then
#   libreoffice --headless --convert-to 'docx' $INPUT_DIR/*.doc --outdir $DOCX_DIR 
# fi
#
# pushd .
#   cd $INPUT_DIR
#   documents=$(ls *.docx | egrep '^[^\.]+' -o)
# popd
#
# for document in $documents; do
#   echo $document is document
#   
#   doc_hash=$(cksum "$INPUT_DIR/${document}.docx" | awk '{print $1}')
#   if [ -e "$DOCX_DIR/${doc_hash}.docx" ]; then
#     echo duplicate $document=$doc_hash
#     continue
#   fi
#
#   echo document name=hash: $document=$doc_hash
#
#   cp "$INPUT_DIR/${document}.docx" "$DOCX_DIR/${doc_hash}.docx"
#
#   pandoc "$INPUT_DIR/${document}.docx" -t plain -o "$TEXT_DIR/${doc_hash}.txt" &> /dev/null
#
#   exiftool "$INPUT_DIR/${document}.docx" -XMP:ALL -XML:ALL -j -n -w "${META_DIR}/%f.json" &> /dev/null
#   if [ ! -e "${META_DIR}/${document}.json" ]
#   then
#     failed_documents="$DOCX_DIR/${doc_hash}.docx $failed_documents" 
#   else
#     mv "$META_DIR/${document}.json" "$META_DIR/${doc_hash}.json"
#   fi 
#
# done
# #
# # for document in $(ls $DOCX_DIR | egrep '^[^\.]+' -o)
# # do
# #   echo $(ls -l "$DOCX_DIR/${document}.docx")
# #   doc_hash=$(cksum "$DOCX_DIR/${document}.docx" | awk '{print $1}')
# #
# #   pandoc "$DOCX_DIR/${document}.docx" -t plain -o "$TEXT_DIR/${doc_hash}.txt" &> /dev/null
# #   exiftool "$DOCX_DIR/${document}.docx" -XMP:ALL -XML:ALL -j -n -w "${META_DIR}/%f.json" &> /dev/null
# #
# #   if [ ! -e "${META_DIR}/${document}.json" ]
# #   then
# #     failed_documents="$DOCX_DIR/${doc_hash}.docx $failed_documents"0 
# #   else
# #     mv "$META_DIR/${document}.json" "$META_DIR/${doc_hash}.json"
# #   fi 
# #
# #   mv "$DOCX_DIR/${document}.docx" "$DOCX_DIR/${doc_hash}.docx"
# #
# #     # tr -s '[[:punct:][:space:]]' ' ' |
# #     # tr '[:upper:]' '[:lower:]' |
# #     # iconv -f utf8 -t ascii//TRANSLIT > "$TEXT_DIR/${document}.txt" 
# # done
# #
# #
# echo Failed documents list: $failed_documents

  # for document_path in "$document_paths"; do
  #   echo "$document_path"
  #   # copy docx doc to WORD_DIR with a new name
  #   document_basename="${document_path%.*}" 
  #   echo "Base: $document_basename<<<"
  #   document_ext="${document_path%%*.}" 
  #   echo "Ext: $document_ext<<<"
  #   # cp "$doucment_path" "$zip_basename-$document_basename"
  # done

