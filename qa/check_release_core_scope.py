#!/usr/bin/env python3
"""Verify restricted cores are absent and retained release native bytes unchanged."""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser()
parser.add_argument('bundle', type=Path)
parser.add_argument('baseline', type=Path)
parser.add_argument('bundled_aar', type=Path)
args = parser.parse_args()
omitted = {'snes9x', 'genesis_plus_gx', 'fbneo', 'mame2003_plus'}
filenames = {f'lib{core}_libretro_android.so' for core in omitted}


def inventory(path):
    with ZipFile(path) as archive:
        return {n: (len(archive.read(n)), hashlib.sha256(archive.read(n)).hexdigest())
                for n in archive.namelist() if n.endswith('.so')}

old = inventory(args.baseline)
new = inventory(args.bundle)
expected = {n: value for n, value in old.items() if Path(n).name not in filenames}
assert new == expected, 'Unexpected native deletion, addition or byte change'
assert len(old) - len(new) == 16, 'Expected four omitted cores across four ABIs'
assert {n.split('/')[2] for n in new} == {'arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'}
for core in ['melonds', 'desmume']:
    assert sum(Path(n).name == f'lib{core}_libretro_android.so' for n in new) == 4
bundled = inventory(args.bundled_aar)
assert not any(Path(n).name in filenames for n in bundled), 'Restricted core remains in QA AAR'
assert len(bundled) == 64, 'Expected sixteen bundled cores across four ABIs'
with ZipFile(args.bundle) as archive:
    assert not any(n.startswith('META-INF/') and n.endswith(('.RSA', '.DSA', '.EC', '.SF'))
                   for n in archive.namelist()), 'Review AAB unexpectedly signed'
print(json.dumps({'result': 'PASS', 'native_entries': len(new),
                  'retained_native_bytes_unchanged': True, 'bundled_core_entries': len(bundled),
                  'abis': ['arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'],
                  'empty_entries_remaining': [n for n, (size, _) in new.items() if size == 0],
                  'bytes': args.bundle.stat().st_size,
                  'sha256': hashlib.sha256(args.bundle.read_bytes()).hexdigest()}, indent=2))
