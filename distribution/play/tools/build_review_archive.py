#!/usr/bin/env python3
"""Package reviewed assets and an external Fastlane upload layout; never upload them."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import zipfile

PACK = Path(__file__).resolve().parents[1]
REPO = PACK.parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--allow-incomplete", action="store_true",
                        help="Prepare a plainly incomplete draft when current native captures are missing")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        raise SystemExit("Refusing to overwrite an existing review archive")
    if PACK == output.parent or PACK in output.parents:
        raise SystemExit("Write review archives outside the source pack")
    check = [sys.executable, str(PACK / "tools/validate_pack.py")]
    if not args.allow_incomplete:
        check.append("--require-complete")
    subprocess.run(check, check=True)
    evidence = json.loads((PACK / "screenshots/evidence.json").read_text())
    eligible = [c for c in evidence["screenshots"] if c.get("listing_eligible") is True]
    output.parent.mkdir(parents=True, exist_ok=True)
    status = {
        "purpose": "Local Play listing review material; no upload, release or publication performed",
        "native_screenshot_minimum_met": len(eligible) >= 2,
        "candidate": evidence.get("candidate"),
        "eligible_native_captures": len(eligible),
        "release_ready": False,
        "remaining_gates": "See distribution/play/README.md, PRIVACY_DATA_SAFETY.md and current candidate runtime report. Sideloaded debug captures do not verify Play delivery.",
    }
    upload_root = Path("external-upload-layout/fastlane/metadata/android/en-US")
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(PACK.rglob("*")):
            if path.is_file() and "__pycache__" not in path.parts and path.suffix != ".pyc":
                archive.write(path, path.relative_to(REPO))
        archive.writestr("REVIEW-STATUS.json", json.dumps(status, indent=2) + "\n")
        for source, target in (("title", "title"), ("short-description", "short_description"),
                               ("full-description", "full_description")):
            archive.write(PACK / "en-US" / f"{source}.txt", str(upload_root / f"{target}.txt"))
        archive.write(PACK / "assets/app-icon.png", str(upload_root / "images/icon.png"))
        archive.write(PACK / "assets/feature-graphic.png", str(upload_root / "images/featureGraphic.png"))
        for capture in eligible:
            archive.write(PACK / "screenshots" / capture["file"],
                          str(upload_root / "images/phoneScreenshots" / capture["file"]))
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_name(output.name + ".sha256").write_text(f"{digest}  {output.name}\n")
    print(f"Review archive: {output}\nBytes: {output.stat().st_size}\nSHA256: {digest}")


if __name__ == "__main__":
    main()
