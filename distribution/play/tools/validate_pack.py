#!/usr/bin/env python3
"""Validate asset/copy structure; --require-complete also rejects missing captures."""

import argparse
import hashlib
import json
from pathlib import Path
import re
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
    candidate = evidence.get("candidate")
    eligible = [capture for capture in captures if capture.get("listing_eligible") is True]
    names = [capture["file"] for capture in captures]
    if len(names) != len(set(names)):
        failures.append("Duplicate screenshot evidence names")
    for capture in captures:
        missing = set(evidence["required_fields_per_capture"]) - capture.keys()
        if missing:
            failures.append(f"Screenshot missing provenance: {sorted(missing)}")
            continue
        asset = PACK / "screenshots" / capture["file"]
        if not re.fullmatch(r"[0-9]{2}-[a-z0-9-]+\.png", capture["file"]):
            failures.append(f"Screenshot needs a plain ordered name: {capture['file']}")
            continue
        if not asset.exists():
            failures.append(f"Screenshot is missing: {asset.name}")
            continue
        if hashlib.sha256(asset.read_bytes()).hexdigest() != capture["sha256"]:
            failures.append(f"Screenshot checksum differs: {asset.name}")
        original = PACK / "screenshots/original-captures" / capture["file"]
        if not original.exists() or hashlib.sha256(original.read_bytes()).hexdigest() != capture["original_capture_sha256"]:
            failures.append(f"Original screenshot missing or checksum differs: {asset.name}")
        elif original.exists():
            with Image.open(original) as source, Image.open(asset) as export:
                if source.size != export.size or source.convert("RGB").tobytes() != export.convert("RGB").tobytes():
                    failures.append(f"Screenshot pixels differ from native capture: {asset.name}")
        if not re.fullmatch(r"[0-9a-f]{64}", capture["apk_sha256"]):
            failures.append(f"Screenshot APK hash is invalid: {asset.name}")
        if capture["capture_method"] != "adb_screencap" or not isinstance(capture["listing_eligible"], bool):
            failures.append(f"Screenshot capture method/eligibility invalid: {asset.name}")
        if not capture["alt_text"] or len(capture["alt_text"]) > 140:
            failures.append(f"Missing/long screenshot alt text: {asset.name}")
        if capture["listing_eligible"]:
            if candidate is None or capture["apk_sha256"] != candidate.get("apk_sha256"):
                failures.append(f"Listing screenshot does not identify the current candidate: {asset.name}")
            elif capture["source_commit_or_tree"] != candidate.get("source_commit_or_tree"):
                failures.append(f"Listing screenshot source differs from the current candidate: {asset.name}")
        with Image.open(asset) as image:
            if image.mode != "RGB" or image.format not in ("PNG", "JPEG"):
                failures.append(f"Screenshot must be RGB PNG/JPEG: {asset.name}")
            if min(image.size) < 320 or max(image.size) > 3840 or max(image.size) > 2 * min(image.size):
                failures.append(f"Screenshot dimension limits exceeded: {asset.name}")
            if image.size != (capture["width"], capture["height"]):
                failures.append(f"Screenshot dimensions differ from provenance: {asset.name}")
    fastlane_screenshots = REPO / "fastlane/metadata/android/en-US/images/phoneScreenshots"
    if any(fastlane_screenshots.glob("*.png")):
        failures.append("Keep Fastlane screenshot upload layouts outside source; the publication guard forbids indexed phoneScreenshots")
    if len(eligible) < 2:
        blocker = evidence.get("capture_blocker", {}).get("reason")
        if blocker:
            print(f"BLOCKED: {blocker}")
        else:
            print("PENDING: fewer than two eligible current native screenshots; pack is not submission complete")
        if args.require_complete:
            failures.append("Verified native screenshots missing" + (f": {blocker}" if blocker else ""))
    if captures:
        print(f"Screenshots: {len(captures)} evidence captures, {len(eligible)} current listing candidates")
    if failures:
        for failure in failures:
            print("FAIL:", failure, file=sys.stderr)
        return 1
    print("PASS: available copy/branding/provenance checks. No release, policy or gameplay certification.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
