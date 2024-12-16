import argparse
import csv
import json
import logging
import os
import re
import subprocess
import sys
import tempfile
import unicodedata
import xml.dom.minidom
import zipfile
from datetime import datetime, date
from pathlib import Path
from typing import List, Dict
from collections import defaultdict
from pathlib import Path
from typing import Dict
from olefile import OleMetadata, olefile

logging.getLogger().setLevel(logging.DEBUG)

# Can not be replaced by simple \w because it matches other slavic character not common to slovak
VALID_CHARACTERS = "\\/01234567789()áäčďéíĺľňóôŕšťúýžabcdefghijklmnopqrstuvwxyz@#$&. ,-_*"
VALID_CHARACTERS_REGEX = re.compile(f"^[{re.escape(VALID_CHARACTERS)}]+$", re.IGNORECASE)

SOURCE_ENCODINGS = ['cp852', 'utf-8', 'cp1252', 'latin2']


def validate_decoded_filename(s: str) -> bool:
    return s and s.isprintable() and VALID_CHARACTERS_REGEX.match(s)


def decode_from_eu_central(data: bytes) -> str | None:
    for encoding in SOURCE_ENCODINGS:
        try:
            decoded = data.decode(encoding)
            normdecoded = unicodedata.normalize('NFC', decoded)
            if validate_decoded_filename(normdecoded):
                return normdecoded

        except UnicodeDecodeError as ignore:
            continue
    return None


def decode_nullable(data: bytes) -> str | None:
    if data:
        return decode_from_eu_central(data)
    return None


def decode_from_cp437(s: str) -> str | None:
    try:
        return decode_from_eu_central(s.encode('cp437'))
    except:
        return None

class Metadata:

    def __init__(self, path):
        self.path = path
        self.filename = os.path.basename(self.path)
        _, self.extension = os.path.splitext(self.path)
        self.extension = self.extension[1:]

        self.pages: str | None = None
        self.template: str | None = None

        self.total_time: int | None = None  # In minutes

        self.creator: str | None = None
        self.last_modified_by: str | None = None

        self.date_created: datetime | None = None
        self.date_modified: datetime | None = None
        self.last_printed: datetime | None = None


def read_metadata(file_path) -> Metadata:
    _, extension = os.path.splitext(file_path)
    try:
        if extension == '.docx':
            return read_metadata_from_docx(file_path)
        elif extension == '.doc':
            return read_metadata_from_doc(file_path)
        else:
          return None

    except Exception as e:
        logging.error(f"Error processing file: {file_path}\nCause {str(e)}")
    return Metadata(file_path)

def read_metadata_from_docx(path: Path) -> Metadata:
    metadata: Metadata = Metadata(path)
    date_format = "%Y-%m-%dT%H:%M:%SZ"  # 2021-12-20T18:41:00Z
    with zipfile.ZipFile(str(path), 'r') as zipf:
        try:
            core = xml.dom.minidom.parseString(zipf.read('docProps/core.xml'))
            metadata.creator = get_dom_element_as_text(core, 'dc:creator')
            metadata.last_modified_by = get_dom_element_as_text(core, 'cp:lastModifiedBy')

            created = get_dom_element_as_text(core, 'dcterms:created')
            metadata.date_created = nullable_str_to_datetime(created, date_format)

            modified = get_dom_element_as_text(core, 'dcterms:modified')
            metadata.date_modified = nullable_str_to_datetime(modified, date_format)

            last_printed = get_dom_element_as_text(core, 'cp:lastPrinted')
            metadata.last_printed = nullable_str_to_datetime(last_printed, date_format)

        except Exception as e:
            logging.warning(f"Document does not have core xml: {path}")

        try:
            app = xml.dom.minidom.parseString(zipf.read('docProps/app.xml'))
            metadata.template = get_dom_element_as_text(app, 'Template')
            totalTime: str = get_dom_element_as_text(app, 'TotalTime')
            metadata.total_time = int(totalTime) if len(totalTime) > 0 else 0

            metadata.pages = get_dom_element_as_text(app, 'Pages')

        except Exception as e:
            logging.warning(f"Document does not have app xml: {path}")

    return metadata


def get_dom_element_as_text(doc, tag_name) -> str | None:
    try:
        return doc.getElementsByTagName(tag_name)[0].childNodes[0].data
    except (IndexError, AttributeError):
        return None


def nullable_str_to_datetime(date: str | None, time_pattern: str) -> datetime | None:
    if date and len(date) > 0:
        return datetime.strptime(date, time_pattern)
    return None


def read_metadata_from_doc(path: Path) -> Metadata:
    metadata = Metadata(path)
    try:
        if not olefile.isOleFile(str(path)):
            logging.warning(f"Path is not a valid DOC file: {path}")
            return metadata

        # date_format = "%Y-%m-%d %H:%M:%s"  # "2021-12-09 20:08:00"
        with olefile.OleFileIO(str(path)) as ofile:

            olemetadata: OleMetadata = ofile.get_metadata()
            metadata.total_time = olemetadata.total_edit_time
            metadata.template = decode_nullable(olemetadata.template)
            metadata.creator = decode_nullable(olemetadata.author)
            metadata.last_modified_by = decode_nullable(olemetadata.last_saved_by)
            metadata.date_created = olemetadata.create_time

            metadata.date_modified = olemetadata.last_saved_time
            metadata.last_printed = olemetadata.last_printed

            metadata.pages = olemetadata.num_pages
            # TODO: move this filtering to results
            if metadata.last_printed and metadata.last_printed.date() < date(1900, 1, 1):
                metadata.last_printed = None

            if metadata.total_time:
                metadata.total_time //= 60

    except Exception as e:
        logging.error(f"Error reading metadata for {path}: {e}")

    return metadata

def collect_metadata(input_dir: Path) -> Dict[Path, Metadata]:
    file_to_metadata = {}
    for document_file in input_dir.iterdir():
        document_meta = read_metadata(document_file) 
        file_to_metadata[str(document_file)] = document_meta

    return file_to_metadata

def find_duplicate_author(file_to_metadata: Dict[Path, Metadata]):
    author_to_file = defaultdict(set)
    for file, meta in file_to_metadata.items():  
        if meta is None:
            continue

        if meta.creator is not None:
            author_to_file[meta.creator].add((file, 'C'))

        if meta.last_modified_by is not None:
            author_to_file[meta.last_modified_by].add((file, 'M'))

    for author, files in author_to_file.items(): 
        print(f"Author: {author}")
        for file in files:
            print(f"\tFile {file[1]}: {file[0]}")

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "input_dir",
        type=Path,
        help="Path to the input directory."
    )

    return parser.parse_args()


def main():
    args = parse_args()

    input_dir = args.input_dir
    if not input_dir.exists() or not input_dir.is_dir():
        print(f"The path provided does not exist or is not a directory: {input_dir}")
        sys.exit(1)

    file_to_metadata = collect_metadata(input_dir)
    find_duplicate_author(file_to_metadata)

if __name__ == "__main__":
    main()
