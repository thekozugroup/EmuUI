#!/usr/bin/env python3
"""Export original EmuUI branding. Requires Inkscape and Pillow; no network access.

The icon paths and colors come from the Android launcher resources. The feature
graphic is original abstract brand artwork, not an app screenshot or device render.
"""

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

from PIL import Image

PACK = Path(__file__).resolve().parents[1]
REPO = PACK.parents[1]
DRAWABLE = REPO / "lemuroid-app/src/main/res/drawable"
ANDROID = "{http://schemas.android.com/apk/res/android}"


def document(width, height, content):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">{content}</svg>\n'
    )


def launcher_icon():
    foreground = ET.parse(DRAWABLE / "emuui_launcher_legacy_foreground.xml").getroot()
    paths = []
    for node in foreground.iter("path"):
        color = node.attrib[ANDROID + "fillColor"]
        opacity = ""
        if len(color) == 9:  # Android #AARRGGBB -> SVG color + alpha
            opacity = f' fill-opacity="{int(color[1:3], 16) / 255:.8f}"'
            color = "#" + color[3:]
        paths.append(f'<path fill="{color}"{opacity} d="{node.attrib[ANDROID + "pathData"]}"/>')
    background = ET.parse(DRAWABLE / "emuui_launcher_background.xml").getroot().find("gradient")
    stops = "".join(
        f'<stop offset="{offset}" stop-color="{background.attrib[ANDROID + attribute]}"/>'
        for offset, attribute in (("0", "startColor"), (".5", "centerColor"), ("1", "endColor"))
    )
    return document(512, 512, (
        f'<defs><linearGradient id="slate" x1="0" y1="0" x2="1" y2="1">{stops}</linearGradient></defs>'
        '<rect width="512" height="512" fill="url(#slate)"/>'
        '<g transform="scale(7.1111111111) translate(-18,-18) translate(54,54) scale(.92) translate(-54,-54)">'
        + "".join(paths) + '</g>'
    ))


def feature_graphic():
    # Text and original pane artwork remain inside a generous central safe zone.
    return document(1024, 500, '''
<defs>
  <linearGradient id="field" x1="0" y1="0" x2="1" y2="1">
    <stop stop-color="#64727C"/><stop offset="1" stop-color="#354650"/>
  </linearGradient>
  <linearGradient id="silver" x1="0" y1="0" x2="0" y2="1">
    <stop stop-color="#F0F4F5"/><stop offset="1" stop-color="#AEBCC5"/>
  </linearGradient>
</defs>
<rect width="1024" height="500" fill="url(#field)"/>
<path d="M-80 445 Q300 260 660 550" fill="none" stroke="#96A9B0" stroke-opacity=".12" stroke-width="56"/>
<path d="M600 -50 Q775 170 1100 65" fill="none" stroke="#AABDC5" stroke-opacity=".10" stroke-width="64"/>
<g font-family="DejaVu Sans, sans-serif" fill="#F3F7F9">
  <text x="102" y="144" font-size="30" font-weight="600" letter-spacing="1">EmuUI</text>
  <text x="97" y="244" font-size="60" font-weight="700" letter-spacing="-2">Your games.</text>
  <text x="97" y="318" font-size="60" font-weight="700" letter-spacing="-2">Room to play.</text>
</g>
<g transform="translate(684 116) rotate(8 90 130)">
  <rect x="8" y="9" width="202" height="114" rx="27" fill="#1D303B" fill-opacity=".28"/>
  <rect x="8" y="149" width="202" height="114" rx="27" fill="#1D303B" fill-opacity=".28"/>
  <rect width="202" height="114" rx="27" fill="url(#silver)"/>
  <rect y="140" width="202" height="114" rx="27" fill="url(#silver)"/>
  <path d="M27 16 H175 M27 156 H175" stroke="#FFFFFF" stroke-opacity=".75" stroke-width="2" stroke-linecap="round"/>
  <path d="M42 58 H160 M42 198 H160" stroke="#485B67" stroke-opacity=".20" stroke-width="24" stroke-linecap="round"/>
</g>''')


def main():
    inkscape = shutil.which("inkscape")
    if inkscape is None:
        raise SystemExit("Inkscape is required for reproducible SVG exports")
    (PACK / "sources").mkdir(exist_ok=True)
    (PACK / "assets").mkdir(exist_ok=True)
    env = dict(os.environ, XDG_CONFIG_HOME="/tmp/emuui-asset-config", XDG_CACHE_HOME="/tmp/emuui-asset-cache")
    for name, svg, mode in (
        ("app-icon", launcher_icon(), "RGBA"),
        ("feature-graphic", feature_graphic(), "RGB"),
    ):
        source = PACK / "sources" / f"{name}.svg"
        output = PACK / "assets" / f"{name}.png"
        source.write_text(svg)
        subprocess.run([inkscape, str(source), "--export-type=png", f"--export-filename={output}"],
                       check=True, capture_output=True, env=env)
        with Image.open(output) as image:
            image.convert(mode).save(output, optimize=True)
        print(f"Exported {output.relative_to(REPO)}")
    entries = []
    for asset in sorted((PACK / "assets").glob("*.png")):
        with Image.open(asset) as image:
            entries.append({"file": asset.name, "sha256": hashlib.sha256(asset.read_bytes()).hexdigest(),
                            "width": image.width, "height": image.height, "mode": image.mode,
                            "bytes": asset.stat().st_size,
                            "kind": "original_brand_artwork_not_app_screenshot"})
    (PACK / "assets" / "manifest.json").write_text(json.dumps({
        "source_launcher_resources": [str(p.relative_to(REPO)) for p in (
            DRAWABLE / "emuui_launcher_background.xml", DRAWABLE / "emuui_launcher_legacy_foreground.xml")],
        "assets": entries,
    }, indent=2) + "\n")


if __name__ == "__main__":
    main()
