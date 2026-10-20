#!/usr/bin/env python3
"""Add a native capture and provenance without cropping, resizing or changing pixels.

Usage: python3 distribution/play/tools/add_capture.py IMAGE.png METADATA.json
Metadata must provide all fields listed in screenshots/evidence.json except fields
derived below. Use listing_eligible=false for supporting evidence that must not enter
the upload set. The original capture is preserved alongside the opaque RGB export.
"""

import argparse
import hashlib
import json
from pathlib import Path
import re

from PIL import Image

PACK = Path(__file__).resolve().parents[1]
DERIVED = {"sha256", "width", "height", "original_capture_sha256", "pixel_processing"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    parser.add_argument("metadata", type=Path)
    args = parser.parse_args()
    evidence_path = PACK / "screenshots/evidence.json"
    evidence = json.loads(evidence_path.read_text())
    capture = json.loads(args.metadata.read_text())
    missing = set(evidence["required_fields_per_capture"]) - DERIVED - capture.keys()
    if missing:
        raise SystemExit(f"Missing capture provenance: {sorted(missing)}")
    filename = capture["file"]
    if not re.fullmatch(r"[0-9]{2}-[a-z0-9-]+\.png", filename):
        raise SystemExit("Use a plain ordered file name, for example 01-foldable-library.png")
    if not re.fullmatch(r"[0-9a-f]{64}", capture["apk_sha256"]):
        raise SystemExit("APK SHA256 must be an exact lowercase 64-character digest")
    if capture["capture_method"] != "adb_screencap":
        raise SystemExit("Only direct native adb_screencap captures are supported")
    if not isinstance(capture["listing_eligible"], bool):
        raise SystemExit("listing_eligible must be true or false")
    if any(c["file"] == filename for c in evidence["screenshots"]):
        raise SystemExit("Capture already exists; preserve existing evidence and use a new ordered name")
    original = args.image.read_bytes()
    with Image.open(args.image) as image:
        if image.format != "PNG" or image.mode not in ("RGB", "RGBA"):
            raise SystemExit("Expected an RGB/RGBA native PNG")
        image.load()
        if image.mode == "RGBA" and image.getchannel("A").getextrema() != (255, 255):
            raise SystemExit("Native capture contains transparency; investigate instead of flattening")
        capture["width"], capture["height"] = image.size
        exported = image.convert("RGB")
        capture["pixel_processing"] = "Opaque RGB format export only; no crop, resize, retouch, UI or gameplay modification"
    capture["original_capture_sha256"] = hashlib.sha256(original).hexdigest()
    original_dir = PACK / "screenshots/original-captures"
    original_dir.mkdir(exist_ok=True)
    original_path = original_dir / filename
    output = PACK / "screenshots" / filename
    if original_path.exists() or output.exists():
        raise SystemExit("Refusing to overwrite an existing capture")
    original_path.write_bytes(original)
    exported.save(output, optimize=True)
    capture["sha256"] = hashlib.sha256(output.read_bytes()).hexdigest()
    evidence["screenshots"].append(capture)
    evidence["screenshots"].sort(key=lambda c: c["file"])
    evidence["status"] = "current_candidate_captures_recorded"
    evidence_path.write_text(json.dumps(evidence, indent=2) + "\n")
    print(json.dumps(capture, indent=2))


if __name__ == "__main__":
    main()
