#!/usr/bin/env python3
"""Validate asset/copy structure; --require-complete also rejects missing captures."""

import argparse
import hashlib
import json
from pathlib import Path
import sys

from PIL import Image

PACK = Path(__file__).resolve().parents[1]
REPO = PACK.parents[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args()
    failures = []
    for name, limit in (("title", 30), ("short-description", 80), ("full-description", 4000)):
        value = (PACK / "en-US" / f"{name}.txt").read_text().strip()
        if not value or len(value) > limit:
            failures.append(f"{name}: {len(value)} characters exceeds {limit} or is empty")
        print(f"{name}: {len(value)}/{limit} characters")
        fastlane_name = name.replace("-", "_") + ".txt"
        if (REPO / "fastlane/metadata/android/en-US" / fastlane_name).read_text().strip() != value:
            failures.append(f"fastlane copy differs: {fastlane_name}")
    alt = json.loads((PACK / "assets/alt-text.json").read_text())
    manifest = json.loads((PACK / "assets/manifest.json").read_text())
    for entry in manifest["assets"]:
        asset = PACK / "assets" / entry["file"]
        expected = ((512, 512), "RGBA") if asset.stem == "app-icon" else ((1024, 500), "RGB")
        with Image.open(asset) as image:
            if (image.size, image.mode) != expected:
                failures.append(f"Wrong size/mode: {asset.name}")
        if asset.name == "app-icon.png" and asset.stat().st_size > 1024 * 1024:
            failures.append("app-icon.png exceeds 1024 KB")
        if hashlib.sha256(asset.read_bytes()).hexdigest() != entry["sha256"]:
            failures.append(f"Checksum differs: {asset.name}")
        if not alt.get(asset.name) or len(alt[asset.name]) > 140:
            failures.append(f"Missing/long alt text: {asset.name}")
    evidence = json.loads((PACK / "screenshots/evidence.json").read_text())
    captures = evidence["screenshots"]
    for capture in captures:
        missing = set(evidence["required_fields_per_capture"]) - capture.keys()
        if missing:
            failures.append(f"Screenshot missing provenance: {sorted(missing)}")
            continue
        asset = PACK / "screenshots" / capture["file"]
        if hashlib.sha256(asset.read_bytes()).hexdigest() != capture["sha256"]:
            failures.append(f"Screenshot checksum differs: {asset.name}")
        with Image.open(asset) as image:
            if image.mode != "RGB" or image.format not in ("PNG", "JPEG"):
                failures.append(f"Screenshot must be RGB PNG/JPEG: {asset.name}")
            if min(image.size) < 320 or max(image.size) > 3840 or max(image.size) > 2 * min(image.size):
                failures.append(f"Screenshot dimension limits exceeded: {asset.name}")
            if image.size != (capture["width"], capture["height"]):
                failures.append(f"Screenshot dimensions differ from provenance: {asset.name}")
    if len(captures) < 2:
        print("PENDING: fewer than two verified native screenshots; pack is not submission complete")
        if args.require_complete:
            failures.append("Verified native screenshots missing")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        return 1
    print("PASS: available copy/branding/provenance checks. No release, policy or gameplay certification.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
