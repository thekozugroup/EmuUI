#!/usr/bin/env python3
"""Fail closed if the proposed Git index contains private or unlicensed assets."""
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
CORE_REVISION = 'fee2e824525daa22bcf318f96127fe43fa8a15ad'
FORBIDDEN_SUFFIXES = {
    '.keystore', '.jks', '.p12', '.pem', '.rom', '.nds', '.gba', '.gb',
    '.gbc', '.nes', '.sfc', '.smc', '.bios', '.elf', '.o', '.class',
    '.dex', '.apk', '.aab', '.log', '.pyc', '.hprof',
}
GENERATED_DIRECTORIES = {'.kotlin', '.gradle', '.qa-output', '__pycache__', 'build'}
MARKERS = [
    b'-----BEGIN PRIVATE KEY-----', b'-----BEGIN RSA PRIVATE KEY-----',
    b'-----BEGIN OPENSSH PRIVATE KEY-----', b'github_pat_', b'ghp_',
]

entries = subprocess.check_output(
    ['git', 'ls-files', '--stage', '-z'], cwd=ROOT,
).split(b'\0')
issues = []
paths = []
for entry in filter(None, entries):
    metadata, raw_name = entry.split(b'\t', 1)
    mode, sha, stage = metadata.decode().split()
    name = raw_name.decode()
    paths.append(name)
    path = pathlib.PurePosixPath(name)
    if stage != '0':
        issues.append(f'Unresolved index conflict: {name}')
    if name == 'lemuroid-cores':
        if mode != '160000' or sha != CORE_REVISION:
            issues.append('Unexpected emulator-core submodule revision or mode')
        continue
    if mode == '160000':
        issues.append(f'Unexpected submodule: {name}')
        continue
    if path.suffix.lower() in FORBIDDEN_SUFFIXES:
        issues.append(f'Forbidden published asset: {name}')
    if any(part in GENERATED_DIRECTORIES for part in path.parts[:-1]):
        issues.append(f'Generated build or QA artifact: {name}')
    if 'phoneScreenshots' in path.parts or path.name == 'local.properties':
        issues.append(f'Private or unlicensed publication asset: {name}')
    # Inspect exactly what Git will publish, even when the working file differs.
    data = subprocess.check_output(['git', 'cat-file', 'blob', sha], cwd=ROOT)
    for marker in MARKERS:
        if marker in data and name != 'qa/check_publication.py':
            issues.append(f'Credential-like marker in {name}')
if 'COPYING' not in paths:
    issues.append('Missing indexed GPL license')
if 'lemuroid-cores' not in paths:
    issues.append('Missing pinned emulator-core submodule')
if issues:
    print('\n'.join(issues))
    sys.exit(1)
print(f'PASS: checked {len(paths)} indexed entries; no forbidden publication assets found')
