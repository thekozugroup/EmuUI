#!/usr/bin/env python3
"""Original CC0 GB/GBC/GBA fixtures, assembled without an external toolchain.

No vendor logo, boot ROM, firmware, commercial code or external art is embedded.
Use a core's built-in/HLE boot path; these are not physical-cartridge boot tests.
Hardware references: https://gbdev.io/pandocs/ and https://mgba-emu.github.io/gbatek/
"""
from pathlib import Path
import argparse
import hashlib
import json
import struct


class GB:
    def __init__(self):
        self.data = bytearray()
        self.labels = {}
        self.fixups = []

    def emit(self, *values):
        self.data.extend(values)

    def label(self, name):
        self.labels[name] = 0x150 + len(self.data)

    def addr(self, opcode, address):
        self.emit(opcode)
        self.data.extend(struct.pack('<H', address))

    def branch(self, opcode, label):
        self.emit(opcode)
        self.fixups.append((len(self.data), label))
        self.emit(0)

    def store(self, address, value):
        self.emit(0x3E, value)
        self.addr(0xEA, address)

    def finish(self):
        for offset, label in self.fixups:
            delta = self.labels[label] - (0x150 + offset + 1)
            if not -128 <= delta <= 127:
                raise ValueError((label, delta))
            self.data[offset] = delta & 255
        return self.data


def build_gb(color=False):
    p = GB()
    p.emit(0xF3)  # DI; the main loop polls LY and needs no interrupt vectors.
    p.addr(0x31, 0xDFFF)
    p.store(0xFF40, 0)
    p.store(0x0000, 0x0A)  # MBC1 RAM enable.
    p.addr(0xFA, 0xA000)
    p.emit(0xFE, 0xA5)
    p.branch(0x28, 'ram_ready')
    p.store(0xA000, 0xA5)
    p.store(0xA001, 0)
    p.label('ram_ready')
    # Clear tile map, VRAM and OAM while LCD is disabled.
    for base, length in [(0x8000, 0x2000), (0xFE00, 160)]:
        p.addr(0x21, base)
        p.addr(0x01, length)
        p.label('clear_' + str(base))
        p.emit(0xAF, 0x22, 0x0B, 0x78, 0xB1)
        p.branch(0x20, 'clear_' + str(base))
    for tile, rows in [(0, [0x55, 0xAA] * 4),
                       (1, [0x18, 0x3C, 0x7E, 0xFF, 0xFF, 0x7E, 0x3C, 0x18]),
                       (2, [0xFF, 0x81, 0xBD, 0xA5, 0xA5, 0xBD, 0x81, 0xFF])]:
        p.addr(0x21, 0x8000 + tile * 16)
        for row in rows:
            p.emit(0x3E, row, 0x22, 0x22)
    for address, value in [(0xFF47, 0xE4), (0xFF48, 0xE4), (0xFF42, 0), (0xFF43, 0),
                           (0xC000, 80), (0xC001, 72), (0xC002, 0), (0xC003, 0xFF),
                           (0xFF26, 0x80), (0xFF24, 0x77), (0xFF25, 0x11),
                           (0xFF11, 0x80), (0xFF12, 0xA0), (0xFF13, 0), (0xFF14, 0x87)]:
        p.store(address, value)
    if color:
        for index_register, data_register in [(0xFF68, 0xFF69), (0xFF6A, 0xFF6B)]:
            p.store(index_register, 0x80)
            for value in [0xFF, 0x7F, 0x40, 0x6E, 0xB4, 0x45, 0x4C, 0x20]:
                p.store(data_register, value)
    p.store(0xFE02, 1)
    p.store(0xFF40, 0x93)
    p.label('frame')
    p.addr(0xFA, 0xFF44)
    p.emit(0xFE, 144)
    p.branch(0x38, 'frame')
    p.addr(0xFA, 0xC002)
    p.emit(0x3C)
    p.addr(0xEA, 0xC002)
    p.store(0xFF00, 0x20)
    p.addr(0xFA, 0xFF00)
    p.addr(0xFA, 0xFF00)
    p.emit(0x47)  # B = active-low direction bits.
    for bit, address, opcode in [(0, 0xC000, 0x3C), (1, 0xC000, 0x3D),
                                 (2, 0xC001, 0x3D), (3, 0xC001, 0x3C)]:
        p.emit(0xCB, 0x40 + bit * 8)
        p.branch(0x20, 'skip_' + str(bit))
        p.addr(0xFA, address)
        p.emit(opcode)
        p.addr(0xEA, address)
        p.label('skip_' + str(bit))
    p.store(0xFF00, 0x10)
    p.addr(0xFA, 0xFF00)
    p.addr(0xFA, 0xFF00)
    p.emit(0x47, 0xCB, 0x40)
    p.branch(0x20, 'a_up')
    p.store(0xFE02, 2)
    p.addr(0xFA, 0xC003)
    p.emit(0xCB, 0x47)
    p.branch(0x28, 'buttons_done')
    p.addr(0xFA, 0xA001)
    p.emit(0x3C)
    p.addr(0xEA, 0xA001)
    p.branch(0x18, 'buttons_done')
    p.label('a_up')
    p.store(0xFE02, 1)
    p.label('buttons_done')
    p.emit(0x78)
    p.addr(0xEA, 0xC003)
    p.addr(0xFA, 0xC001)
    p.addr(0xEA, 0xFE00)
    p.addr(0xFA, 0xC000)
    p.addr(0xEA, 0xFE01)
    p.label('wait_visible')
    p.addr(0xFA, 0xFF44)
    p.emit(0xFE, 144)
    p.branch(0x30, 'wait_visible')
    p.addr(0xC3, p.labels['frame'])
    rom = bytearray(32768)
    rom[0x100:0x104] = bytes([0x00, 0xC3, 0x50, 0x01])
    rom[0x134:0x13F] = b'EMUUI GB QA'
    rom[0x143] = 0x80 if color else 0
    rom[0x147] = 0x03  # MBC1+RAM+BATTERY.
    rom[0x149] = 0x02  # 8 KiB SRAM.
    rom[0x14A] = 1
    rom[0x150:0x150 + len(p.finish())] = p.data
    rom[0x14D] = (-sum(rom[0x134:0x14D]) - 25) & 255
    checksum = sum(rom) & 65535
    rom[0x14E:0x150] = struct.pack('>H', checksum)
    return bytes(rom)


class ARM:
    def __init__(self):
        self.words = []
        self.labels = {}
        self.branches = []
        self.literals = []

    def emit(self, word):
        self.words.append(word)

    def label(self, name):
        self.labels[name] = len(self.words)

    def literal(self, reg, value):
        self.literals.append((len(self.words), reg, value))
        self.emit(0)

    def branch(self, name, cond=14, link=False):
        self.branches.append((len(self.words), name, cond, link))
        self.emit(0)

    def mov(self, dest, value, cond=14):
        assert 0 <= value <= 255
        self.emit((cond << 28) | 0x03A00000 | (dest << 12) | value)

    def finish(self):
        for offset, name, cond, link in self.branches:
            self.words[offset] = (cond << 28) | 0x0A000000 | (int(link) << 24) | ((self.labels[name] - offset - 2) & 0xFFFFFF)
        for offset, reg, value in self.literals:
            distance = (len(self.words) - offset - 2) * 4
            assert 0 <= distance < 4096
            self.words[offset] = 0xE59F0000 | (reg << 12) | distance
            self.words.append(value)
        return struct.pack('<' + 'I' * len(self.words), *self.words)


def build_gba():
    p = ARM()
    p.literal(0, 0x04000000)
    p.literal(1, 0x0403)  # Mode 3 + BG2, visible bitmap.
    p.emit(0xE1C010B0)  # STRH r1,[r0].
    p.literal(0, 0x06000000)
    p.literal(1, 0x62C8)
    p.literal(2, 240 * 160)
    p.label('clear')
    p.emit(0xE0C010B2)  # STRH r1,[r0],#2.
    p.emit(0xE2522001)  # SUBS r2,r2,#1.
    p.branch('clear', 1)
    p.literal(0, 0x04000084)
    p.mov(1, 0x80)
    p.emit(0xE1C010B0)
    for address, value in [(0x04000080, 0x1177), (0x04000082, 0x0002),
                           (0x04000060, 0), (0x04000062, 0xA080), (0x04000064, 0x8700)]:
        p.literal(0, address)
        p.literal(1, value)
        p.emit(0xE1C010B0)
    p.mov(4, 112)
    p.mov(5, 72)
    p.literal(10, 0x0E000000)
    p.emit(0xE5DA0000)  # LDRB r0,[r10].
    p.emit(0xE35000A5)
    p.branch('ram_ready', 0)
    p.mov(0, 0xA5)
    p.emit(0xE5CA0000)
    p.mov(0, 0)
    p.emit(0xE5CA0001)
    p.label('ram_ready')
    p.mov(11, 0xFF)
    p.label('frame')
    p.literal(0, 0x04000006)
    p.label('wait_vblank')
    p.emit(0xE1D010B0)  # LDRH r1,[r0].
    p.emit(0xE35100A0)
    p.branch('wait_vblank', 11)  # BLT.
    p.emit(0xE1A00004)
    p.emit(0xE1A01005)
    p.literal(2, 0x62C8)
    p.branch('draw', link=True)
    p.literal(0, 0x04000130)
    p.emit(0xE1D030B0)  # R3 = KEYINPUT.
    for mask, instruction in [(16, 0x02844001), (32, 0x02444001),
                               (64, 0x02455001), (128, 0x02855001)]:
        p.emit(0xE3130000 | mask)  # TST r3,#mask.
        p.emit(instruction)  # Conditional EQ ADD/SUB x/y.
    # Bounds keep a long button hold on-screen.
    p.emit(0xE3540004)
    p.mov(4, 4, 11)
    p.emit(0xE35400E0)
    p.mov(4, 224, 12)
    p.emit(0xE3550004)
    p.mov(5, 4, 11)
    p.emit(0xE3550090)
    p.mov(5, 144, 12)
    p.emit(0xE3130001)
    p.branch('no_a', 1)
    p.emit(0xE31B0001)
    p.branch('no_a', 0)
    p.emit(0xE5DA0001)
    p.emit(0xE2800001)
    p.emit(0xE5CA0001)
    p.label('no_a')
    p.emit(0xE1A0B003)
    p.literal(2, 0x2152)
    p.emit(0xE3130001)
    p.literal(0, 0x5D7F)
    p.emit(0x01A02000)  # MOVEQ r2,r0.
    p.emit(0xE1A00004)
    p.emit(0xE1A01005)
    p.branch('draw', link=True)
    p.literal(0, 0x04000006)
    p.label('wait_visible')
    p.emit(0xE1D010B0)
    p.emit(0xE35100A0)
    p.branch('wait_visible', 10)  # BGE.
    p.branch('frame')
    p.label('draw')
    p.literal(3, 0x06000000)
    p.mov(8, 240)
    p.emit(0xE0090198)  # MUL r9,r8,r1.
    p.emit(0xE0899000)
    p.emit(0xE0833089)  # ADD r3,r3,r9,LSL#1.
    p.mov(9, 12)
    p.label('row')
    p.mov(8, 12)
    p.label('pixel')
    p.emit(0xE0C320B2)  # STRH r2,[r3],#2.
    p.emit(0xE2588001)
    p.branch('pixel', 1)
    p.literal(8, 456)
    p.emit(0xE0833008)
    p.emit(0xE2599001)
    p.branch('row', 1)
    p.emit(0xE12FFF1E)  # BX LR.
    rom = bytearray(192)
    rom[:4] = struct.pack('<I', 0xEA00002E)
    rom[0xA0:0xAC] = b'EMUUI GBA QA'
    rom[0xAC:0xB0] = b'EUQA'
    rom[0xB2] = 0x96
    rom[0xBD] = (-sum(rom[0xA0:0xBD]) - 0x19) & 255
    rom.extend(p.finish())
    rom.extend(b'SRAM_V113\0')
    rom.extend(bytes(32768 - len(rom)))
    return bytes(rom)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path(__file__).parent / 'generated')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    manifest = {'license': 'CC0-1.0', 'vendor_logo': False, 'external_bios': False, 'files': []}
    for name, data in [('EmuUI_GB_QA.gb', build_gb()), ('EmuUI_GBC_QA.gbc', build_gb(True)), ('EmuUI_GBA_QA.gba', build_gba())]:
        (args.output / name).write_bytes(data)
        manifest['files'].append({'name': name, 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(manifest, indent=2))


if __name__ == '__main__':
    main()
