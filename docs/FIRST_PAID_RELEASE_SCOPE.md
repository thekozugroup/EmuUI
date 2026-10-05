# First paid release scope — authorized 2026-10-05

User response to restricted-core choices: “we can look for replacements or just disregard them.” This authorizes bounded replacement assessment and omission within the restricted SNES/Sega/arcade group. It does not authorize ABI narrowing, other system removals, or signing/account/security actions.

## Assessment and decision

| Candidate | Evidence | Why not integrated in this release |
| --- | --- | --- |
| bsnes-libretro for SNES | GPL-3.0-or-later root license; maintained libretro build recipe | No validated EmuUI Android multi-ABI build, native gameplay fixture, performance or save migration results. Root licensing alone does not complete dependency clearance. |
| Gearsystem for Master System/Game Gear | GPL project, Android libretro support and `platforms/libretro/jni/Android.mk` | Most practical follow-up candidate, but no validated gameplay/save compatibility results in this app. Does not replace Genesis/Sega CD. |
| Modern MAME for the two arcade backends | Modern source licensed GPL-2.0-or-later with per-file terms | Different ROM sets and runtime/performance profile; no validated Android integration or compatibility evidence. Old MAME2003+ licensing remains restricted. |

Official sources reviewed: https://github.com/libretro/bsnes-libretro/blob/master/LICENSE.txt ; https://github.com/libretro/bsnes-libretro/blob/master/Makefile ; https://github.com/drhelius/Gearsystem ; https://github.com/drhelius/Gearsystem/blob/master/platforms/libretro/jni/Android.mk ; https://www.mamedev.org/legal.html . This was a bounded source/build-recipe assessment, not a claim that candidate builds or gameplay passed. No candidate code is integrated. No newly installed software or paid service was used.

Use the authorized omission fallback instead of delaying the first paid release for unverified replacements.

## Exact changes

Removed executable modules: Snes9x, Genesis Plus GX, FBNeo, MAME2003+. Their binaries are excluded from both Play dynamic feature packaging and the bundled QA flavor. The pinned upstream submodule is unmodified.

Unavailable systems: SNES/Super Famicom; Sega Master System; Sega Game Gear; Sega Genesis/Mega Drive; Sega CD/Mega-CD; FBNeo arcade; MAME2003+ arcade. No replacements are claimed. Nintendo DS and its melonDS/DeSmuME configurations remain intact. All four ABIs remain; the unrelated four empty Citra/PPSSPP entries remain blocked pending the separate support decision.

Historical system/core IDs and save-sync names remain readable. New scans do not import omitted systems; launch paths reject their existing entries with an explanatory message; background core updates skip them. No save/state/preview deletion or conversion is introduced. Rescans may remove unavailable library database entries through the existing index cleanup; external ROMs and save files are not deleted by this change. Save-state compatibility with future replacement cores is not promised.

## Listing handoff

Remove SNES, Master System, Game Gear, Genesis/Mega Drive, Sega CD and arcade support from the first paid release description, feature lists, screenshots/captions and promotional art. Do not publish screenshots suggesting those systems are playable. Keep DS claims scoped to the existing horizontal-hinge UI. 3DS was already blocked by the app's split-screen UI; do not advertise it as playable simply because its native module remains packaged. No listing was edited remotely.

Remaining source obligations for other cores are unchanged (Citra dirty changes, WonderSwan revision, exact ABI provenance, PPSSPP assets/static sources). Removing those systems would need separate authorization. This scope change does not establish that the remaining paid candidate is cleared or launch-ready.

## Validation of this revision

PASS: production AAB, release lint (0 errors, 49 warnings and 4 hints), 201 app unit tests (including 3 new scope/DS/history regression tests), bundled-core AAR and bundletool validation. Initial build exposed Java17/Kotlin21 target mismatch in native feature modules; fixed by pinning Kotlin target17 consistently. Failed initial log is retained.

Archive-level check `qa/check_release_core_scope.py` passed: exactly16 native entries removed (four restricted cores across four ABIs), 72 remaining AAB entries, all retained native bytes identical to the previous candidate, all four ABIs retained, both DS cores retained on all four ABIs. Bundled QA AAR contains64 core entries with none of the restricted cores. No release signature.

New unsigned review AAB: 94192496 bytes, SHA256 `fc774473cff672ed26a3c9c9a1c011b5efa33c6f2b065c4b83695d6140bd1972`. Still blocked by the four empty Citra/PPSSPP files and remaining gates. Earlier native16KB/NES/UI results refer to retained identical native binaries and prior frontend revision; no fresh instrumented/UI gameplay run or replacement-core gameplay is claimed for this scope change.
