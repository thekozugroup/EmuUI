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
| `screenshots/evidence.json` | Final APK identity, exact capture blocker and future per-capture provenance | Blocked by Android system-server watchdog before final app installation; zero current captures |
| `screenshots/capture-plan.md` | Honest phone-guidance/foldable gameplay capture sequence | Not executed on Android |
| `PRIVACY_DATA_SAFETY.md` | Source-based behavior and exact final release-manifest reconciliation | Inventory, not an approved form |
| `release-manifest-inventory.json` | AAB identity, exact permissions, backup/service configuration and audit references | Matched to unsigned release AAB `eaba8193…`; no network/SDK certification |
| `privacy-policy-draft.md` | Human-readable policy text for final publisher review | Contact, URL and service-retention details remain open |

The inherited Lemuroid listing and TV banner were removed/replaced. EmuUI has no TV launcher and does not offer ordinary-phone gameplay. No commercial gameplay screenshots, device renders, generated gameplay or screenshots from a different binary are included.

## Regenerate and check

Requires Python 3, Pillow and Inkscape. Rendering uses local SVG/vector resources only.

```sh
python3 distribution/play/tools/render_assets.py
python3 distribution/play/tools/validate_pack.py
# This must fail while native screenshots remain missing:
python3 distribution/play/tools/validate_pack.py --require-complete
# After current captures pass, create a fresh archive outside source:
python3 distribution/play/tools/build_review_archive.py /workspace/EmuUI-Play-Candidate-Review.zip
```

The checker validates copy lengths, Fastlane parity, PNG sizes/modes, checksums, alt text and recorded screenshot provenance. It does not certify compatibility, image rights, final merged permissions or policy compliance.

For current native captures, first set the `candidate` object in `screenshots/evidence.json` to the actual installed APK's SHA-256, source commit/tree, version, build flavor and delivery type. Then use `tools/add_capture.py NATIVE.png METADATA.json` with the required per-capture provenance fields from that manifest. The importer preserves the original, accepts only direct opaque native PNGs, and exports RGB without cropping, scaling or changing pixels. Captures stay in this pack: the repository publication guard intentionally forbids indexed Fastlane `phoneScreenshots` paths. A Fastlane upload layout may be prepared only in an external review archive. That group can include open foldable phones and does not imply ordinary-phone gameplay. Supporting captures can remain in evidence with `listing_eligible=false`.

Current QA uses `freeBundleDebug`, version `0.3.1-dev.20261020-DEBUG`. A sideloaded debug screenshot must be labeled accordingly; it cannot prove the separately built unsigned `PlayDynamicRelease` AAB or Google Play feature delivery. Record software CPU/GPU emulation, API, native resolution and all synthetic hinge/posture inputs. A current ordinary-phone or cover-display capture may demonstrate open-screen guidance, and must not be described as supported phone gameplay.

The review-archive tool refuses to overwrite earlier archives. It includes provenance, original native captures and a separate Fastlane upload-layout directory inside the external ZIP. Only captures explicitly marked eligible for the current APK enter that layout; no image upload is performed. Its `--allow-incomplete` option is for an explicitly unfinished draft, and `REVIEW-STATUS.json` always distinguishes listing asset completeness from launch readiness.

## Asset requirements checked

Google specifies a 512×512 32-bit PNG icon of at most 1024 KB, and a 1024×500 JPEG or opaque 24-bit PNG feature graphic. Screenshots use JPEG or opaque PNG, with at least two images; each dimension is 320–3840 pixels and the long edge is no more than twice the short edge. Larger promotional placements have additional resolution/aspect recommendations. Preserve actual foldable geometry. See [Play preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) and [icon specification](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), checked 20 October 2026.

Keep the square icon uncropped: Google applies its own corner treatment. The feature graphic is branding, clearly separate from the required native captures. Neither claims certification of a device, emulator core or game.

## Submission remains blocked

Native screenshot capture is blocked. On the fresh isolated API 30 AVD, `boot_complete` was reached but Android Watchdog killed `system_server` at **20 October 2026, 18:39:05 UTC** for blocked foreground/UI handlers, before the final app could be installed. Gradle was stopped during that fresh run. The initial AVD also had system-process restarts and failed installs. These observations establish a cloud Android runtime blocker, not a demonstrated app crash or Android test failure. There are **zero current app screenshots and zero executed Android tests**. The exact blocker and evidence references are recorded in `screenshots/evidence.json`; no historical October 10 image, host framebuffer, generated gameplay or mockup is substituted.

The new external `EmuUI-Play-Review-Incomplete-20261020.zip` is explicitly incomplete: `nativeMinimum=false`, `releaseReady=false` and `uploadScreenshotCount=0`. Earlier draft ZIPs remain unchanged. Complete final-binary gameplay/lifecycle/audio/foldable acceptance, signed Play-delivery testing, source/license distribution obligations, publisher-approved privacy URL/contact and Data safety review before publication. The pack contains no private signing material and does not authorize upload, publication, merge or release.
