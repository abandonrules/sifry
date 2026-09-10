# Raw data sources for `gen_canon.py`

Run `python3 gen_canon.py` from this directory; it regenerates
`spa2/src/main/assets/raw/{periodic,pokemon,wordle}.canon.gz`.

The files here were fetched from (see `lic_sources.txt` for full credit):

- `wordle.csv` — https://github.com/steve-kasica/wordle-words
  (raw: `https://raw.githubusercontent.com/steve-kasica/wordle-words/master/wordle.csv`)
  Columns: `word,occurrence,day`.
- `pokemon.csv` — https://github.com/cristobalmitchell/pokedex (MIT)
  (raw: `https://raw.githubusercontent.com/cristobalmitchell/pokedex/main/data/pokemon_utf8.csv`)
  Columns include `national_number,english_name,primary_type,secondary_type`.
- `periodic.csv` — Periodic Table of Elements gist by GoodmanSciences
  (raw: `https://gist.githubusercontent.com/GoodmanSciences/c2dd862cd38f21b0ad36b8f96b4bf1ee/raw/32d1227bbd086f640b25b390ae380a15f25d4a64/Periodic%20Table%20of%20Elements.csv`)
  Columns include `AtomicNumber,Element,Symbol`.

## Canon format

Every line is `key:Display`. `key` is the lowercase normalized form (ASCII
`[a-z]` only — diacritics, digits and punctuation stripped), `Display` is the
full original form returned by Dictionary Search. Sources are gzipped to
`*.canon.gz`; the Android build unpacks them back to plain `*.canon` assets,
which is what the native PCRE search reads.