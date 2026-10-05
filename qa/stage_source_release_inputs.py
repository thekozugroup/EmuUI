#!/usr/bin/env python3
"""Stage hash-locked local release inputs from the delivered source archives. No network."""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('deliverables', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
cores = json.loads((root / 'qa/source-core-binaries.json').read_text())
assets = json.loads((root / 'qa/source-core-assets.json').read_text())

def save(data, relative, expected):
    assert hashlib.sha256(data).hexdigest() == expected, f'Hash mismatch: {relative}'
    path = root / '.release-native' / relative
    if path.exists():
        assert path.read_bytes() == data, f'Refusing to overwrite different input: {path}'
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print(f'PASS {relative}')

with ZipFile(args.deliverables / 'Source-built-release-native-inputs.zip') as source:
    for filename, expected in cores.items():
        relative = Path('arm64-v8a') / filename
        save(source.read(str(relative)), relative, expected)
    save(source.read('assets/core-assets/ppsspp.zip'),
         Path('assets/core-assets/ppsspp.zip'), assets['core-assets/ppsspp.zip'])
