#!/usr/bin/env python3
"""Read-only Android build prerequisites. Never installs tools or accepts agreements."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil


def inspect(root, sdk, java_home, release):
    findings = []

    def check(name, path, expected_hash=None):
        exists = path.is_file()
        status = "available" if exists else "missing"
        if exists and expected_hash:
            actual = hashlib.sha256(path.read_bytes()).hexdigest()
            status = "verified" if actual == expected_hash else "hash-mismatch"
        findings.append({"requirement": name, "path": str(path), "status": status})

    versions = (root / "buildSrc/src/main/java/deps.kt").read_text()
    platform = re.search(r'compileSdkVersion\s*=\s*(\d+)', versions).group(1)
    build_tools = re.search(r'buildToolsVersion\s*=\s*"([^"]+)"', versions).group(1)
    renderer = (root / "libretrodroid/build.gradle.kts").read_text()
    ndk = re.search(r'ndkVersion\s*=\s*"([^"]+)"', renderer).group(1)
    cmake = re.search(r'version\s*=\s*"([^"]+)"', renderer).group(1)
    check("JDK compiler", java_home / "bin/javac")
    check("Android platform", sdk / f"platforms/android-{platform}/android.jar")
    check("Android build tools", sdk / f"build-tools/{build_tools}/aapt2")
    check("Android NDK", sdk / f"ndk/{ndk}/source.properties")
    check("Android CMake", sdk / f"cmake/{cmake}/bin/cmake")
    check("Pinned core submodule", root / "lemuroid-cores/bundled-cores/build.gradle.kts")
    if release:
        native = root / ".release-native"
        for name, expected in json.loads((root / "qa/source-core-binaries.json").read_text()).items():
            check(f"Source-built core: {name}", native / "arm64-v8a" / name, expected)
        for name, expected in json.loads((root / "qa/source-core-assets.json").read_text()).items():
            check(f"Bundled core asset: {name}", native / "assets" / name, expected)
    return {
        "scope": "play-or-bundled" if release else "free-dynamic",
        "ready": all(item["status"] in ("available", "verified") for item in findings),
        "checks": findings,
        "limits": [
            "Availability is not a build, runtime, signing, license or Play acceptance result.",
            "SDK agreements must be reviewed and accepted by an authorized person.",
            "Never replace hash-locked release inputs with unrelated upstream binaries.",
        ],
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, default=Path(os.environ.get("ANDROID_HOME", "/nonexistent-android-sdk")))
    default_java = Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else Path(shutil.which("java") or "/nonexistent/bin/java").resolve().parents[1]
    parser.add_argument("--java-home", type=Path, default=default_java)
    parser.add_argument("--release", action="store_true", help="Also verify every locked Play/bundled native input")
    args = parser.parse_args()
    result = inspect(Path(__file__).resolve().parents[1], args.sdk, args.java_home, args.release)
    print(json.dumps(result, indent=2))
    raise SystemExit(0 if result["ready"] else 1)
