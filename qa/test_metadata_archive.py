"""Verify the lossless representation of the unchanged upstream metadata asset."""
import gzip
import hashlib
import io
import pathlib
import unittest

ARCHIVE_DIRECTORY = pathlib.Path(__file__).resolve().parents[1] / (
    "lemuroid-metadata-libretro-db/src/main/metadata"
)
EXPECTED_ARCHIVE_SHA256 = "4811f938c06cbbdd49ca7c5ee60cee2f0e4b7359802b3cf52cc78af1adec680c"
EXPECTED_ARCHIVE_SIZE = 3_376_283
EXPECTED_SIZE = 12_775_424
EXPECTED_SHA256 = "4c724302254bef897b248c0e538b8039a731e5e1dc7c0bdc6c255b9407b196d1"


class MetadataArchiveTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        paths = sorted(ARCHIVE_DIRECTORY.glob("libretro-db.sqlite.gz.part-*"))
        if sum(path.stat().st_size for path in paths) != EXPECTED_ARCHIVE_SIZE:
            raise AssertionError("Unexpected compressed metadata size")
        cls.compressed = b"".join(
            path.read_bytes() for path in paths
        )
        if hashlib.sha256(cls.compressed).hexdigest() != EXPECTED_ARCHIVE_SHA256:
            raise AssertionError("Compressed metadata checksum mismatch")
        with gzip.GzipFile(fileobj=io.BytesIO(cls.compressed)) as archive:
            cls.database = archive.read(EXPECTED_SIZE + 1)
        if len(cls.database) > EXPECTED_SIZE:
            raise AssertionError("Metadata expands beyond its expected size")

    def test_exact_archive(self):
        self.assertEqual(EXPECTED_ARCHIVE_SHA256, hashlib.sha256(self.compressed).hexdigest())

    def test_exact_upstream_database(self):
        self.assertEqual(EXPECTED_SIZE, len(self.database))
        self.assertEqual(EXPECTED_SHA256, hashlib.sha256(self.database).hexdigest())

    def test_sqlite_header(self):
        self.assertEqual(b"SQLite format 3\0", self.database[:16])

    def test_truncated_archive_rejected(self):
        with self.assertRaises((EOFError, OSError)):
            gzip.decompress(self.compressed[:-8])

    def test_corrupt_checksum_rejected(self):
        corrupt = bytearray(self.compressed)
        corrupt[-8] ^= 1
        with self.assertRaises(OSError):
            gzip.decompress(corrupt)


if __name__ == "__main__":
    unittest.main()
