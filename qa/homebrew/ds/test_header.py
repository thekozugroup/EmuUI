"""Header compatibility regressions. SPDX-License-Identifier: CC0-1.0."""
import struct
import unittest
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from header import crc16, homebrew_logo, validate_header, validate_segments


class HeaderTests(unittest.TestCase):
    def fixture_header(self):
        data = bytearray(0x200)
        data[0x0C:0x10] = b"####"
        data[0xC0:0x15C] = homebrew_logo()
        struct.pack_into("<H", data, 0x15C, crc16(data[0xC0:0x15C]))
        struct.pack_into("<H", data, 0x15E, crc16(data[:0x15E]))
        return data

    def test_original_blank_field_has_required_checksum_without_artwork(self):
        logo = homebrew_logo()
        self.assertEqual(156, len(logo))
        self.assertEqual(bytes(154), logo[:154])
        self.assertLessEqual(sum(byte != 0 for byte in logo), 2)
        self.assertEqual(0xCF56, crc16(logo))

    def test_literal_zero_field_reproduces_observed_melonds_rejection(self):
        self.assertEqual(0xE155, crc16(bytes(156)))

    def test_complete_header_is_valid(self):
        validate_header(self.fixture_header())

    def test_retail_shaped_game_code_is_rejected(self):
        data = self.fixture_header()
        data[0x0C:0x10] = b"EUIQ"
        struct.pack_into("<H", data, 0x15E, crc16(data[:0x15E]))
        with self.assertRaisesRegex(ValueError, "homebrew game code"):
            validate_header(data)

    def test_wrong_stored_logo_crc_is_rejected(self):
        data = self.fixture_header()
        data[0x15C] ^= 1
        with self.assertRaisesRegex(ValueError, "logo CRC16"):
            validate_header(data)

    def test_corrupt_header_is_rejected(self):
        data = self.fixture_header()
        data[0x10] ^= 1
        with self.assertRaisesRegex(ValueError, "header CRC16"):
            validate_header(data)

    def test_nonoriginal_logo_is_rejected(self):
        data = self.fixture_header()
        data[0xC0] = 1
        with self.assertRaisesRegex(ValueError, "Unexpected bytes"):
            validate_header(data)

    def test_truncated_header_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Truncated"):
            validate_header(b"QA")

    def segment_fixture(self):
        data = bytearray(0x500)
        struct.pack_into("<I", data, 0x84, 0x200)
        struct.pack_into("<4I", data, 0x20, 0x200, 0x02000000, 0x02000000, 0x100)
        struct.pack_into("<4I", data, 0x30, 0x300, 0x02380000, 0x02380000, 0x100)
        return data

    def test_segments_have_valid_bounds_and_entry_points(self):
        self.assertEqual({"arm9", "arm7"}, set(validate_segments(self.segment_fixture())))

    def test_truncated_arm_segment_is_rejected(self):
        data = self.segment_fixture()
        struct.pack_into("<I", data, 0x2C, 0xFFFF)
        with self.assertRaisesRegex(ValueError, "arm9 ROM segment bounds"):
            validate_segments(data)

    def test_outside_entry_point_is_rejected(self):
        data = self.segment_fixture()
        struct.pack_into("<I", data, 0x34, 0x01000000)
        with self.assertRaisesRegex(ValueError, "arm7 entry point"):
            validate_segments(data)

    def test_overlapping_segments_are_rejected(self):
        data = self.segment_fixture()
        struct.pack_into("<I", data, 0x30, 0x280)
        with self.assertRaisesRegex(ValueError, "overlap"):
            validate_segments(data)

    def test_manifest_records_legacy_runtime_and_arm7_source(self):
        self.check_manifest("legacy", False)

    def test_manifest_records_calico_runtime(self):
        self.check_manifest("calico", True)

    def check_manifest(self, runtime, expect_calico):
        data = self.segment_fixture()
        data[0x0C:0x10] = b"####"
        data[0xC0:0x15C] = homebrew_logo()
        struct.pack_into("<H", data, 0x15C, crc16(data[0xC0:0x15C]))
        struct.pack_into("<H", data, 0x15E, crc16(data[:0x15E]))
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            rom = root / "original.nds"
            source = root / "original.c"
            arm7 = root / "arm7.c"
            rom.write_bytes(data)
            source.write_text("original source")
            arm7.write_text("adapted licensed ARM7 source")
            subprocess.run([
                sys.executable, str(Path(__file__).with_name("header.py")),
                "validate", str(rom), str(source), "--runtime", runtime,
                "--arm7-source", str(arm7),
            ], check=True, capture_output=True)
            manifest = json.loads(rom.with_suffix(".manifest.json").read_text())
            self.assertEqual(runtime, manifest["runtime"])
            self.assertEqual(expect_calico, any("Calico" in item for item in manifest["linked_components"]))
            self.assertEqual(hashlib.sha256(arm7.read_bytes()).hexdigest(), manifest["arm7_source_sha256"])
            self.assertEqual(hashlib.sha256(data).hexdigest(), manifest["sha256"])


if __name__ == "__main__":
    unittest.main()
