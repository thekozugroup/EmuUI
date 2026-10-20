# Native screenshot capture plan

Blocked before final app installation: the fresh cloud AVD's Android Watchdog killed `system_server` on 20 October 2026 at 18:39:05 UTC for blocked foreground/UI handlers. No Android test or current app capture executed. No images in this directory count as store screenshots. `evidence.json` records the final APK identity, runtime limits and supporting evidence paths. Resume this plan only in a stable Android runtime; do not replace it with historical images or host framebuffer renders.

Capture clean PNGs directly from the app with `adb exec-out screencap -p`, using the final tested binary. Record every field in `evidence.json`. Confirm SHA-256 on the installed APK and label emulator captures as emulator captures. Disable notification content and use original or explicitly licensed homebrew only. Do not hide an error with image editing or upscale an old capture to imply a device was tested.

| Order | Capture | Device/posture | Acceptance before capture |
| --- | --- | --- | --- |
| 1 | Populated library with original QA games | Open foldable AVD, horizontal fold, landscape | Cards, filter selection and Add action are legible; no licensed cover art |
| 2 | Original single-screen homebrew with controls | Same foldable AVD | Actual load/input verified on this APK; game rights recorded |
| 3 | Library, alternate filter | Foldable AVD, half-open | Synthetic hinge excluded from panes; state survives posture event |
| 4 | Original DS homebrew and lower touchscreen | Foldable AVD, unfolded landscape | Actual native output; no UI overlap; correctly labeled as an AVD |
| 5 | Settings or save slots | Same foldable AVD | Actual settings/save data; no personal paths or accounts |
| 6 | Open-screen and rotate guidance | Ordinary phone and portrait foldable | Guidance readable; returning to valid fold restores state |

The supported gameplay posture requires a reported horizontal fold; do not invent ordinary-phone gameplay screenshots. A phone screenshot may accurately show the open-screen guidance, but should not lead the listing. Use the native inner-display resolution and record any synthetic fold injection. A foldable screenshot may have a different aspect ratio from Google's promotional 16:9 recommendation; preserve native geometry instead of stretching or cropping out functional controls. The general Play dimension limits still apply. A synthetic fold test demonstrates layout behavior and cannot substantiate OEM or physical foldable claims. The Play screenshot requirements are linked from the pack README.
