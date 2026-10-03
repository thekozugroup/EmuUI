# Library metadata asset

The source contains the unchanged upstream SQLite database as one gzip stream stored in seven sequential pieces of at most 512 KiB. Gradle concatenates them in filename order, verifies the compressed checksum, decompresses offline into generated Android assets, and checks the original size and SHA-256 before publishing the generated file. The APK still contains the ordinary `assets/libretro-db.sqlite`; no runtime decompression, network fetch or new dependency is added.

This lossless representation keeps repository transfers smaller. Existing attribution and licenses remain applicable.

- Upstream repository: [Swordfish90/Lemuroid](https://github.com/Swordfish90/Lemuroid)
- Upstream commit: `53752bf29bc3f95c50f6c38f70cd4a53450a7098`
- Original Git blob: `5ebd6edff6f5c0d895f056133194f1f62906d555`
- Uncompressed bytes: `12775424`
- Uncompressed SHA-256: `4c724302254bef897b248c0e538b8039a731e5e1dc7c0bdc6c255b9407b196d1`
- Concatenated gzip SHA-256: `4811f938c06cbbdd49ca7c5ee60cee2f0e4b7359802b3cf52cc78af1adec680c`

`PrepareLibretroMetadata` is a cacheable Gradle task with declared file/checksum inputs and a generated output directory. A bad, truncated or oversized archive fails the build before replacing an existing verified output. Standard Android builds generate the asset automatically.

To verify the checked-in archive directly, run `python3 -m unittest discover -s qa -p test_metadata_archive.py` from the repository root. The parts can also be concatenated with ordinary file tools and opened by any gzip reader. Regeneration uses Python's `gzip.compress(original_bytes, compresslevel=9, mtime=0)`, then 524288-byte slices named `libretro-db.sqlite.gz.part-000` onward. A different gzip implementation can change compressed bytes and therefore requires reviewing the compressed checksum; the uncompressed pin must continue to match the documented upstream asset unless metadata is intentionally updated and reviewed.
