#!/usr/bin/env python3
"""Regenerate SPA2's supplemental .canon dictionaries from raw data sources.

Canon format (matches cs.canon / en.canon / what Dictionary Search reads):
one entry per line  key:Display  where key is the lowercase [a-z] lookup form
(diacritics, digits and punctuation stripped) and Display is the full
original form shown in search results. The files are gzipped as *.canon.gz;
the Android build unpacks them back to plain .canon assets.

Input files (see data/README.md for where to fetch them):
  data/wordle.csv      steve-kasica/wordle-words
  data/pokemon.csv     cristobalmitchell/pokedex  (pokemon_utf8.csv)
  data/periodic.csv    GoodmanSciences periodic-table gist

Writes spa2/src/main/assets/raw/{periodic,pokemon,wordle}.canon.gz
"""
import csv
import gzip
import os
import re
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(HERE, "..", "..", "spa2", "src", "main", "assets", "raw"))


def key_of(display: str) -> str:
    s = unicodedata.normalize("NFKD", display).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z]", "", s.lower())


def make_canon(rows, out_name):
    seen = set()
    lines = []
    for display, k in rows:
        k = key_of(k)
        if not k or (k, display) in seen:
            continue
        seen.add((k, display))
        lines.append((k, display))
    lines.sort()
    data = "".join(f"{k}:{d}\n" for k, d in lines)
    path = os.path.join(ASSETS, out_name + ".gz")
    with gzip.open(path, "wt", encoding="utf-8", newline="\n") as f:
        f.write(data)
    print(f"{out_name}: {len(lines)} entries, {len(data)/1e6:.2f} MB raw, "
          f"{os.path.getsize(path)/1e6:.2f} MB gz")


def main():
    # Wordle words
    wordle = []
    with open(os.path.join(HERE, "data", "wordle.csv"), newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            w = row["word"].strip().lower()
            if w:
                wordle.append((w, w))
    make_canon(wordle, "wordle.canon")

    # Pokemón names/types
    pkm = []
    with open(os.path.join(HERE, "data", "pokemon.csv"), newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            name = row["english_name"].strip()
            if not name:
                continue
            types = "/".join(t.title() for t in (row["primary_type"], row["secondary_type"]) if t)
            display = f"{name} ({types}) #{row['national_number']}" if types else f"{name} #{row['national_number']}"
            pkm.append((display, name))
    make_canon(pkm, "pokemon.canon")

    # Periodic table: element name + symbol keys
    per = []
    with open(os.path.join(HERE, "data", "periodic.csv"), newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            el = row["Element"].strip()
            sym = row["Symbol"].strip()
            num = row["AtomicNumber"].strip()
            if not el or not sym:
                continue
            display = f"{el} ({sym}, {num})"
            per.append((display, el))
            per.append((display, sym))
    make_canon(per, "periodic.canon")


if __name__ == "__main__":
    main()