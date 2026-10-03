#!/usr/bin/env python3
"""Original, artwork-free DS homebrew header generator. SPDX-License-Identifier: CC0-1.0."""
import argparse
import hashlib
import json
from pathlib import Path
import struct


def crc16(data: bytes, initial: int = 0xFFFF) -> int:
    """DS header CRC-16 (reflected polynomial 0xA001)."""
    result = initial
    for value in data:
        result ^= value
        for _ in range(8):
            result = (result >> 1) ^ (0xA001 if result & 1 else 0)
    return result


def homebrew_logo() -> bytes:
    """156 original bytes: 154 zeros plus a generated two-byte checksum adjustment.

    Retain the conventional DS-format CRC16 0xCF56 without including or recreating
    Nintendo artwork: only two bytes can be nonzero. melonDS DS skips the fixed
    logo-checksum check for correctly classified #### homebrew; our validator
    additionally verifies both checksums for a self-consistent test artifact.
    """
    prefix = bytes(154)
    initial = crc16(prefix)
    for candidate in range(0x10000):
        suffix = candidate.to_bytes(2, "little")
        if crc16(suffix, initial) == 0xCF56:
            return prefix + suffix
    raise ValueError("No homebrew checksum suffix found")


def validate_header(data: bytes) -> None:
    if len(data) < 0x160:
        raise ValueError("Truncated DS header")
    # With a modern 0x4000-byte header, melonDS distinguishes homebrew by ####.
    # A made-up retail game code would cause an unencrypted image to be classified
    # as encrypted retail software and needlessly require a proprietary BIOS.
    if data[0x0C:0x10] != b"####":
        raise ValueError("Missing homebrew game code")
    if data[0xC0:0x15C] != homebrew_logo():
        raise ValueError("Unexpected bytes in original homebrew logo field")
    logo_crc, header_crc = struct.unpack_from("<HH", data, 0x15C)
    if logo_crc != 0xCF56 or crc16(data[0xC0:0x15C]) != logo_crc:
        raise ValueError("Invalid logo CRC16")
    if crc16(data[:0x15E]) != header_crc:
        raise ValueError("Invalid header CRC16")


def validate_segments(data: bytes) -> dict:
    if len(data) < 0x160:
        raise ValueError("Truncated DS header")
    header_size = struct.unpack_from("<I", data, 0x84)[0]
    if header_size < 0x160 or header_size > len(data):
        raise ValueError("Invalid header size")
    segments = {}
    spans = []
    for name, position in (("arm9", 0x20), ("arm7", 0x30)):
        offset, entry, address, size = struct.unpack_from("<4I", data, position)
        if size == 0 or offset < header_size or offset + size > len(data):
            raise ValueError(f"Invalid {name} ROM segment bounds")
        if address + size > 0x100000000 or not address <= (entry & ~1) < address + size:
            raise ValueError(f"Invalid {name} entry point")
        spans.append((offset, offset + size))
        segments[name] = {"rom_offset": hex(offset), "entry": hex(entry),
                          "load_address": hex(address), "bytes": size}
    if max(spans[0][0], spans[1][0]) < min(spans[0][1], spans[1][1]):
        raise ValueError("ARM ROM segments overlap")
    return segments


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    logo = sub.add_parser("logo")
    logo.add_argument("output", type=Path)
    validate = sub.add_parser("validate")
    validate.add_argument("rom", type=Path)
    validate.add_argument("source", type=Path)
    validate.add_argument("--runtime", choices=("calico", "legacy"), default="calico")
    validate.add_argument("--arm7-source", type=Path)
    args = parser.parse_args()
    if args.command == "logo":
        args.output.write_bytes(homebrew_logo())
        return
    data = args.rom.read_bytes()
    validate_header(data)
    segments = validate_segments(data)
    manifest = {
        "name": "EmuUI DS QA", "filename": args.rom.name,
        "sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data),
        "source_sha256": hashlib.sha256(args.source.read_bytes()).hexdigest(),
        "header_generator_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        "original_source_license": "CC0-1.0",
        "header_logo": "154 zero bytes and a generated two-byte CRC16 adjustment; no artwork",
        "logo_crc16": "cf56",
        "game_code": "####",
        "homebrew_classification": "Conventional #### game code; original ARM code is not encrypted",
        "header_crc16": f"{struct.unpack_from('<H', data, 0x15E)[0]:04x}",
        "segments": segments,
        "runtime": args.runtime,
        "linked_components": (
            ["libnds 1.8.3 (zlib)", "adapted devkitPro ARM7 service (zlib)", "devkitARM CRT (MPL-2.0)"]
            if args.runtime == "legacy" else ["libnds 2.0.2 (zlib)", "Calico 1.2.0 (ZPL-2.1)"]
        ) + ["newlib (per-file permissive licenses)", "GCC runtime (GPL-3.0 with runtime-library exception)"],
        "contains_commercial_rom_bios_firmware_assets": False,
        "purpose": "Local-only Nintendo DS emulator dual-screen, touch, controls and state QA",
    }
    if args.arm7_source:
        manifest["arm7_source_sha256"] = hashlib.sha256(args.arm7_source.read_bytes()).hexdigest()
    args.rom.with_suffix(".manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
