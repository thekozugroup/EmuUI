#!/usr/bin/env python3
"""Checks store assets against Google Play, Galaxy Store and App Store limits before upload."""
import pathlib
import re
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
STORE = ROOT / 'store'
FASTLANE = ROOT / 'fastlane/metadata/android/en-US'
BANNED = re.compile(r"\b(best|#1|top|new|free|discount|sale|million downloads|no ads|editor'?s choice|download now)\b", re.I)
problems = []


def image(path, size=None, alpha_ok=False, max_kb=None, ratio=None):
    if not path.exists():
        problems.append(f'missing {path.relative_to(ROOT)}')
        return
    im = Image.open(path)
    name = path.relative_to(ROOT)
    if size and im.size != size:
        problems.append(f'{name}: {im.size}, expected {size}')
    if not alpha_ok and ('A' in im.getbands() or im.mode == 'P'):
        problems.append(f'{name}: has an alpha channel or palette')
    if max_kb and path.stat().st_size > max_kb * 1024:
        problems.append(f'{name}: larger than {max_kb} KB')
    w, h = im.size
    if ratio and max(w, h) > ratio * min(w, h):
        problems.append(f'{name}: aspect {max(w, h) / min(w, h):.2f} exceeds {ratio}:1')
    if min(w, h) < 320 or max(w, h) > 3840:
        problems.append(f'{name}: sides must be 320 to 3840 px')


def screenshots(folder, size, minimum, maximum, ratio):
    files = sorted(folder.glob('*.png'))
    if not minimum <= len(files) <= maximum:
        problems.append(f'{folder.relative_to(ROOT)}: {len(files)} images, expected {minimum} to {maximum}')
    for f in files:
        image(f, size=size, ratio=ratio)


def text(path, limit, unit='chars', banned=True):
    if not path.exists():
        problems.append(f'missing {path.relative_to(ROOT)}')
        return
    value = path.read_text(encoding='utf-8').rstrip('\n')
    length = len(value.encode('utf-8')) if unit == 'bytes' else len(value)
    if length > limit:
        problems.append(f'{path.relative_to(ROOT)}: {length} {unit}, limit {limit}')
    if banned and BANNED.search(value):
        problems.append(f'{path.relative_to(ROOT)}: promotional word "{BANNED.search(value).group(0)}"')


# Google Play: 16:9 sets qualify for featuring; large-screen sets carry no added text.
screenshots(STORE / 'play/phone', (1920, 1080), 4, 8, 2)
screenshots(STORE / 'play/sevenInch', (1920, 1080), 4, 8, 2)
screenshots(STORE / 'play/tenInch', (2560, 1440), 4, 8, 2)
image(STORE / 'play/featureGraphic.png', size=(1024, 500))
image(STORE / 'play/icon.png', size=(512, 512), alpha_ok=True, max_kb=1024)
image(STORE / 'play/tvBanner.png', size=(1280, 720))
text(FASTLANE / 'title.txt', 30)
text(FASTLANE / 'short_description.txt', 80)
# The full description's "no ads" and "free software" are factual, so only its length is checked.
text(FASTLANE / 'full_description.txt', 4000, banned=False)
for log in (FASTLANE / 'changelogs').glob('*.txt'):
    text(log, 500)

# Samsung Galaxy Store: 4 to 8 screenshots, no wider than 2:1, text limits in bytes.
screenshots(STORE / 'galaxy/screenshots', None, 4, 8, 2)
image(STORE / 'galaxy/icon.png', size=(512, 512), alpha_ok=True, max_kb=1024)
text(STORE / 'galaxy/title.txt', 100, 'bytes')
text(STORE / 'galaxy/short_description.txt', 40, 'bytes')
text(STORE / 'galaxy/long_description.txt', 4000, 'bytes', banned=False)

# Apple App Store copy (no screenshots: see store/README.md).
text(STORE / 'app-store/name.txt', 30)
text(STORE / 'app-store/subtitle.txt', 30)
text(STORE / 'app-store/promotional_text.txt', 170)
text(STORE / 'app-store/description.txt', 4000, banned=False)
keywords = STORE / 'app-store/keywords.txt'
text(keywords, 100, 'bytes')
if keywords.exists():
    words = keywords.read_text(encoding='utf-8').strip().split(',')
    if any(w != w.strip() or len(w.strip()) <= 2 for w in words):
        problems.append('store/app-store/keywords.txt: no spaces after commas, keywords longer than 2 characters')

# Neither store copy may mention another platform's device brand.
for path in [*FASTLANE.glob('*.txt'), *STORE.glob('*/*.txt')]:
    if re.search(r'\bpixel\b|\biphone\b|\bgalaxy\b', path.read_text(encoding='utf-8'), re.I):
        problems.append(f'{path.relative_to(ROOT)}: names a device brand')

if problems:
    print('\n'.join(problems))
    sys.exit(1)
print('PASS: store assets are within Google Play, Galaxy Store and App Store limits')
