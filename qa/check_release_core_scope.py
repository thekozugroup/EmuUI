#!/usr/bin/env python3
"""Verify restricted cores are absent and retained release native bytes unchanged."""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser()
parser.add_argument('--arm64-only', action='store_true')
parser.add_argument('--replacement-lock', type=Path)
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
expected = {n: value for n, value in old.items() if Path(n).name not in filenames and (not args.arm64_only or '/arm64-v8a/' in n)}
assert new.keys() == expected.keys(), 'Unexpected native deletion or addition'
replacements = json.loads(args.replacement_lock.read_text()) if args.replacement_lock else {}
changed = []
for name, value in new.items():
    replacement = replacements.get(Path(name).name)
    if replacement:
        assert value[1] == replacement, f'Replacement hash mismatch: {name}'
        changed.append(Path(name).name)
    else:
        assert value == expected[name], f'Unexpected native byte change: {name}'
assert set(changed) == set(replacements), 'Missing requested replacement core'
assert len(new) == (18 if args.arm64_only else 72), 'Unexpected final native count'
expected_abis = {'arm64-v8a'} if args.arm64_only else {'arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'}
assert {n.split('/')[2] for n in new} == expected_abis
assert not args.arm64_only or all(size > 0 for size, _ in new.values())
for core in ['melonds', 'desmume']:
    assert sum(Path(n).name == f'lib{core}_libretro_android.so' for n in new) == len(expected_abis)
bundled = inventory(args.bundled_aar)
assert not any(Path(n).name in filenames for n in bundled), 'Restricted core remains in QA AAR'
assert len(bundled) == 16 * len(expected_abis), 'Unexpected bundled native count'
release_by_name = {Path(n).name: value for n, value in new.items()}
for n, value in bundled.items():
    if Path(n).name in replacements:
        assert value == release_by_name[Path(n).name], f'QA/release replacement mismatch: {n}'
with ZipFile(args.bundle) as archive:
    assert not any(n.startswith('META-INF/') and n.endswith(('.RSA', '.DSA', '.EC', '.SF'))
                   for n in archive.namelist()), 'Review AAB unexpectedly signed'
print(json.dumps({'result': 'PASS', 'native_entries': len(new),
                  'retained_native_bytes_unchanged_except_locked_replacements': True,
                  'source_replacements': sorted(changed), 'bundled_core_entries': len(bundled),
                  'abis': sorted(expected_abis),
                  'empty_entries_remaining': [n for n, (size, _) in new.items() if size == 0],
                  'bytes': args.bundle.stat().st_size,
                  'sha256': hashlib.sha256(args.bundle.read_bytes()).hexdigest()}, indent=2))
