"""Format and assembler bounds checks for original QA fixtures. CC0-1.0."""
import struct
import unittest

from build_fixtures import GB, build_gb, build_gba


class FixtureFormatTest(unittest.TestCase):
    def test_gb_header_checksum_matches_header(self):
        for color in (False, True):
            rom = build_gb(color)
            self.assertEqual(len(rom), 32768)
            self.assertEqual((sum(rom[0x134:0x14E]) + 25) & 255, 0)

    def test_gb_global_checksum_matches_contents(self):
        for color in (False, True):
            rom = build_gb(color)
            expected = (sum(rom[:0x14E]) + sum(rom[0x150:])) & 65535
            self.assertEqual(struct.unpack('>H', rom[0x14E:0x150])[0], expected)

    def test_gb_declares_battery_ram_and_color_mode(self):
        for color in (False, True):
            rom = build_gb(color)
            self.assertEqual(rom[0x143], 0x80 if color else 0)
            self.assertEqual(rom[0x147:0x14A], b'\x03\x00\x02')

    def test_no_vendor_boot_logo(self):
        self.assertEqual(build_gb()[0x104:0x134], bytes(48))
        self.assertEqual(build_gba()[4:0xA0], bytes(156))

    def test_gba_entry_skips_header(self):
        rom = build_gba()
        branch = struct.unpack('<I', rom[:4])[0]
        self.assertEqual(branch >> 24, 0xEA)
        self.assertEqual(((branch & 0xFFFFFF) << 2) + 8, 0xC0)

    def test_gba_header_checksum_and_save_signature(self):
        rom = build_gba()
        self.assertEqual((sum(rom[0xA0:0xBE]) + 0x19) & 255, 0)
        self.assertEqual(rom[0xB2], 0x96)
        self.assertIn(b'SRAM_V113\0', rom)
        self.assertEqual(len(rom), 32768)

    def test_generator_is_deterministic(self):
        for build in (build_gb, lambda: build_gb(True), build_gba):
            self.assertEqual(build(), build())

    def test_relative_branch_out_of_range_is_rejected(self):
        assembly = GB()
        assembly.branch(0x18, 'far')
        assembly.emit(*bytes(130))
        assembly.label('far')
        with self.assertRaises(ValueError):
            assembly.finish()


if __name__ == '__main__':
    unittest.main()
