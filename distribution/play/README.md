# EmuUI Play listing draft

Prepared 20 October 2026 from the current EmuUI development branch, preserving the metallic launcher identity and open-foldable console contract. This is a local review pack, not a published listing or a claim of Google approval.

## Deliverables

| File | Purpose | Status |
| --- | --- | --- |
| `assets/app-icon.png` | 512×512 RGBA store icon, exported from the current Android launcher shapes/colors | Rendered and inspected |
| `assets/feature-graphic.png` | 1024×500 RGB original abstract brand artwork | Rendered and inspected; not an app screenshot |
| `sources/*.svg` | Editable vector sources | Generated locally; no third-party game art |
| `assets/manifest.json` / `assets/alt-text.json` | Checksums, dimensions, accessible descriptions | Validated |
| `en-US/*.txt` | App title, short/full listing copy | Mirrors `fastlane/metadata/android/en-US` |
| `screenshots/evidence.json` | Per-capture APK/source/device/posture/rights provenance | Pending native capture |
| `screenshots/capture-plan.md` | Honest phone-guidance/foldable gameplay capture sequence | Pending execution |
| `PRIVACY_DATA_SAFETY.md` | Source-based behavior and unresolved submission choices | Inventory, not an approved form |
| `privacy-policy-draft.md` | Human-readable policy text for final publisher review | Contact, URL and service-retention details remain open |

The inherited Lemuroid listing and TV banner were removed/replaced. EmuUI has no TV launcher and does not offer ordinary-phone gameplay. No commercial gameplay screenshots, device renders, generated gameplay or screenshots from a different binary are included.

## Regenerate and check

Requires Python 3, Pillow and Inkscape. Rendering uses local SVG/vector resources only.

```sh
python3 distribution/play/tools/render_assets.py
python3 distribution/play/tools/validate_pack.py
# This must fail while native screenshots remain missing:
python3 distribution/play/tools/validate_pack.py --require-complete
```

The checker validates copy lengths, Fastlane parity, PNG sizes/modes, checksums, alt text and recorded screenshot provenance. It does not certify compatibility, image rights, final merged permissions or policy compliance.

## Asset requirements checked

Google specifies a 512×512 32-bit PNG icon of at most 1024 KB, and a 1024×500 JPEG or opaque 24-bit PNG feature graphic. Screenshots use JPEG or opaque PNG, with at least two images; each dimension is 320–3840 pixels and the long edge is no more than twice the short edge. Larger promotional placements have additional resolution/aspect recommendations. Preserve actual foldable geometry. See [Play preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) and [icon specification](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), checked 20 October 2026.

Keep the square icon uncropped: Google applies its own corner treatment. The feature graphic is branding, clearly separate from the required native captures. Neither claims certification of a device, emulator core or game.

## Submission remains blocked

Native screenshot capture of the final APK is pending. Also complete final-binary gameplay/lifecycle/audio/foldable acceptance, signed Play-delivery testing, source/license distribution obligations, publisher-approved privacy URL/contact and Data safety review. The pack does not contain signing credentials and does not authorize upload, publication, merge or release.
