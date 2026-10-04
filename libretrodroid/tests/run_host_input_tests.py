#!/usr/bin/env python3
"""Test the actual Input and pulse scheduler with only Android constants stubbed."""
import os
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CPP = ROOT / "src/main/cpp"
JAVA = ROOT / "src/main/java/com/swordfish/libretrodroid"

# Runtime correctness depends on placement inside the actual retro_run loop,
# not merely after a GL draw or asynchronous/replayed FrameRendered event.
native = (CPP / "libretrodroid.cpp").read_text()
assert re.search(
    r"for \(size_t i = 0; i < frames \* frameSpeed; i\+\+\)\s*\{\s*"
    r"core->retro_run\(\);\s*if \(input\) input->afterEmulatedFrame\(\);\s*\}", native,
)
for method in ("enqueueAccessibleTap", "cancelAccessibleTaps"):
    assert f"if (input) input->{method}(port, keyCode);" in native
assert "input = nullptr;" in native.split("void LibretroDroid::pause()", 1)[1].split("void LibretroDroid::step()", 1)[0]
view = (JAVA / "GLRetroView.kt").read_text()
enqueue = view.split("fun enqueueAccessibleTap", 1)[1].split("fun cancelAccessibleTaps", 1)[0]
assert "if (!isEmulationReady) return" in enqueue
assert "isEmulationReady && accessibleInputGeneration.isCurrent(ticket)" in enqueue
pause = view.split("private fun pause()", 1)[1].split("inner class Renderer", 1)[0]
assert pause.index("accessibleInputGeneration.invalidate()") < pause.index("onPause()")
destroy = view.split("fun onDestroy()", 1)[1].split("private fun getDeviceLanguage", 1)[0]
assert destroy.index("accessibleInputGeneration.invalidate()") < destroy.index("LibretroDroid.destroy()")
assert "if (keyCode == null) accessibleInputGeneration.invalidate()" in view
touch = (ROOT.parent / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/shared/game/viewmodel/GameViewModelTouchControls.kt").read_text()
assert "override fun onPause(owner: LifecycleOwner) {\n        releaseVirtualControls()" in touch
assert "delay(120)" not in touch
assert "accessiblePressJobs" not in touch
print("PASS: real-run advancement, paused-core guards and lifecycle/gate generation wiring", flush=True)

with tempfile.TemporaryDirectory(prefix="emuui-input-test-") as temporary:
    work = Path(temporary)
    (work / "android").mkdir()
    # Android platform numeric constants only; input state/mapping/pulse logic
    # below is compiled directly from production Input, without behavioral mocks.
    (work / "android/input.h").write_text(
        "#pragma once\nenum { AKEY_EVENT_ACTION_DOWN = 0, AKEY_EVENT_ACTION_UP = 1 };\n"
    )
    keys = {
        "DPAD_UP": 19, "DPAD_DOWN": 20, "DPAD_LEFT": 21, "DPAD_RIGHT": 22,
        "BUTTON_A": 96, "BUTTON_B": 97, "BUTTON_X": 99, "BUTTON_Y": 100,
        "BUTTON_L1": 102, "BUTTON_R1": 103, "BUTTON_L2": 104, "BUTTON_R2": 105,
        "BUTTON_THUMBL": 106, "BUTTON_THUMBR": 107, "BUTTON_START": 108, "BUTTON_SELECT": 109,
        "DPAD_UP_LEFT": 268, "DPAD_DOWN_LEFT": 269, "DPAD_UP_RIGHT": 270, "DPAD_DOWN_RIGHT": 271,
    }
    (work / "android/keycodes.h").write_text(
        "#pragma once\nenum {\n" + ",\n".join(f"AKEYCODE_{key} = {value}" for key, value in keys.items()) + "\n};\n"
    )
    binary = work / "inputpulse_test"
    subprocess.run([
        os.environ.get("CXX", "c++"), "-std=c++17", "-Wall", "-Wextra", "-pedantic",
        "-I", str(CPP), "-I", str(CPP / "libretro/libretro-common/include"), "-I", str(work),
        str(ROOT / "tests/inputpulse_test.cpp"), str(CPP / "input.cpp"), "-o", str(binary),
    ], check=True)
    subprocess.run([str(binary)], check=True)
