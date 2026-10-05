#!/usr/bin/env python3
"""Inventory every native ELF, including symlinks; static checks are not runtime certification."""
import argparse
import hashlib
import json
from pathlib import Path
import struct


def inspect(path):
    data = path.read_bytes()
    if data[:4] != b'\x7fELF':
        return dict(path=str(path), bytes=len(data), sha256=hashlib.sha256(data).hexdigest(),
                    error='Not ELF (including empty ABI placeholders)', loads=[], relro=[],
                    load_alignment_pass=False, relro_boundary_review=False, runtime_tested=False)
    bits = data[4]
    order = '<' if data[5] == 1 else '>'
    if bits == 2:
        offset = struct.unpack_from(order + 'Q', data, 32)[0]
        size, count = struct.unpack_from(order + 'HH', data, 54)
        fmt = order + 'IIQQQQQQ'
    elif bits == 1:
        offset = struct.unpack_from(order + 'I', data, 28)[0]
        size, count = struct.unpack_from(order + 'HH', data, 42)
        fmt = order + 'IIIIIIII'
    else:
        raise ValueError(f'Unsupported ELF class: {bits}')
    loads, relro = [], []
    for i in range(count):
        fields = struct.unpack_from(fmt, data, offset + i * size)
        if bits == 2:
            kind, flags, file_offset, address, _, file_size, memory_size, alignment = fields
        else:
            kind, file_offset, address, _, file_size, memory_size, flags, alignment = fields
        if kind == 1:
            loads.append(dict(offset=file_offset, address=address, alignment=alignment,
                              aligned_16kb=alignment >= 16384 and (address-file_offset) % 16384 == 0))
        if kind == 0x6474e552:
            relro.append(dict(address=address, memory_size=memory_size,
                              end_mod_16kb=(address+memory_size) % 16384))
    return dict(path=str(path), bytes=len(data), sha256=hashlib.sha256(data).hexdigest(),
                elf_bits=32 if bits == 1 else 64, loads=loads, relro=relro,
                load_alignment_pass=bool(loads) and all(p['aligned_16kb'] for p in loads),
                relro_boundary_review=any(p['end_mod_16kb'] for p in relro), runtime_tested=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    args = parser.parse_args()
    rows = [inspect(p) for p in sorted(args.directory.rglob('*.so'))]
    print(json.dumps(dict(libraries=rows, count=len(rows),
                         load_alignment_failures=sum(not p['load_alignment_pass'] for p in rows),
                         relro_boundary_review=sum(p['relro_boundary_review'] for p in rows)), indent=2))
