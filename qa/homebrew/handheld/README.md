# Original Game Boy family QA fixtures

`build_fixtures.py` assembles three original small test programs with Python 3:
`EmuUI_GB_QA.gb`, `EmuUI_GBC_QA.gbc` and `EmuUI_GBA_QA.gba`. No toolchain,
external art, game code, vendor logo, BIOS or firmware is copied into the programs.
The sources and generated programs are dedicated to the public domain under
[CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/), without warranty.
The libretro cores used to run them retain their own licenses.

The default output directory, `generated/`, is ignored by Git. Do not package the
fixtures into the app or represent their host-core tests as Android gameplay tests.
Use a core's built-in or HLE boot path. The blank vendor-logo fields deliberately
do not authenticate through an original retail boot ROM. Real-cartridge boot and
BIOS validation are outside this fixture's scope.

```sh
python3 qa/homebrew/handheld/build_fixtures.py
python3 -m unittest discover -s qa/homebrew/handheld -p 'test_*.py'
```

Game Boy and Game Boy Color show a patterned background and a small player diamond.
The D-pad moves the player; holding A changes it to an outlined square. Game Boy
Advance shows a teal background and a brown square; the D-pad moves it and holding
A changes it to pink. GBA movement remains within the display. These programs also
produce a continuous square-wave test tone. Keep test volume comfortable.

Each distinct A press increments a battery-save counter once. The first two save
RAM bytes are the original sentinel `A5` and that counter. This is a developer
assertion, not a player-facing file format. Both GB variants declare MBC1 with
8 KiB battery RAM; the GBA fixture declares SRAM and uses 32 KiB after detection.
The GB counter at WRAM offset 2 also increments as frames run. No Nintendo logos
or commercial text/graphics are displayed.

## Repeatable host preflight

`verify_libretro.py` accepts a separately built **host** libretro core. It does not
download software. Tests assert load, nonuniform rendering, four D-pad directions,
A input, nonzero audio samples, matching visible player output after a state-file
round trip, serialization stability while `retro_run` is withheld, advancement on
resume, and battery RAM reloaded from disk into a new core session. A held A press
must leave the battery counter at 1. An empty system directory supplies no BIOS.

```sh
python3 qa/homebrew/handheld/verify_libretro.py \
  --core /outside/repository/gambatte_libretro.so \
  --rom qa/homebrew/handheld/generated/EmuUI_GB_QA.gb \
  --work-dir /outside/repository/gb-test \
  --report /outside/repository/gb-test.json
```

Repeat for `.gbc` with Gambatte and `.gba` with `mgba_libretro.so`. mGBA initially
reports its maximum save allocation before detecting SRAM; the harness restores
the persisted RAM prefix into that allocation before guest execution. It does not
resize or rewrite the save file. The host tests do not establish that EmuUI uses
the same restoration behavior; Android app persistence must be tested separately.

The executed 20 October 2026 host results and exact source/build pins are in
`host-verification.json`. Both cores were also rebuilt from the source revisions
recorded for EmuUI's Android cores, using their unmodified upstream Makefiles:

```sh
git clone https://github.com/libretro/gambatte-libretro.git /tmp/gambatte-host
git -C /tmp/gambatte-host checkout b75225203ffea8b65124bb31acb598e91e7f22d9
make -C /tmp/gambatte-host -j4 platform=unix
git clone https://github.com/libretro/mgba.git /tmp/mgba-host
git -C /tmp/mgba-host checkout c758314a639aa0066e7b65a8341448181b73c804
make -C /tmp/mgba-host -f Makefile.libretro -j4
```

These are still host builds, **not** EmuUI's pinned Android core binaries. The
harness writes an unmodified framebuffer PNG with `.host-framebuffer.png` in its
filename for inspection. It is not an app or Google Play screenshot. Nonzero audio
samples establish signal generation, not audibility, quality, latency or routing.
Withholding `retro_run` establishes core pause stability, not Android lifecycle
pause/resume. The file round trips do not establish crash-safe Android writes.

## Native app acceptance still required

Import each generated file through the app's normal folder picker, then verify
the expected display and every visible control. Pause and resume from the app and
Android background/foreground transitions; listen for audio stopping and resuming.
Save a distinct position, move, load, and compare the restored position and A state.
Leave normally, force-stop, reopen and verify both autosave and battery persistence.
Repeat during rotate, window resize and actual fold/open transitions. Capture
screenshots only from the app being tested and record its APK hash, core hash,
device/API level, run steps and limitations. These checks remain separate from
the host preflight.

Hardware references used for register behavior are [Pan Docs](https://gbdev.io/pandocs/)
and [GBATEK](https://mgba-emu.github.io/gbatek/). No reference source code was copied.
