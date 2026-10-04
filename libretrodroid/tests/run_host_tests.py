#!/usr/bin/env python3
"""Exercise actual pure renderer/layout code without Android, NDK or an emulator."""
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CPP = ROOT / "src/main/cpp"

# Tie the pure duplicate-frame regression to every actual Video mutation path.
video_source = (CPP / "video.cpp").read_text(encoding="utf-8")
mutations = [
    "updateScreenSize", "updateViewportSize", "updateAspectRatio", "updateRotation",
    "updateRenderRegions", "updateRendererSize", "updateShaderType",
]
for mutation in mutations:
    body = re.search(r"void Video::" + mutation + r"\([^)]*\)\s*\{(.*?)\n\}", video_source, re.S)
    assert body and "presentationState.invalidate();" in body.group(1), mutation
assert "if (!presentationState.consumeFrame(skipDuplicateFrames)) return;" in video_source
render_body = video_source.split("void Video::renderFrame()", 1)[1].split("float Video::getScreenDensity", 1)[0]
assert "updateProgram();" not in render_body  # Do not switch resources on duplicate frames.
new_frame_body = video_source.split("void Video::onNewFrame", 1)[1].split("void Video::updateScreenSize", 1)[0]
assert new_frame_body.index("updateProgram();") < new_frame_body.index("renderer->onNewFrame")
framebuffer_source = (CPP / "renderers/es3/framebufferrenderer.cpp").read_text(encoding="utf-8")
shader_update = framebuffer_source.split("void FramebufferRenderer::setShaders", 1)[1].split("Renderer::PassData", 1)[0]
assert "initializeShaderPasses();" in shader_update
assert "initializeBuffers();" not in shader_update and "isDirty = true" not in shader_update
assert video_source.index("glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)") < video_source.index("if (!hasFrame) return;")
bridge_source = (ROOT / "src/main/java/com/swordfish/libretrodroid/GLRetroView.kt").read_text(encoding="utf-8")
initialize = bridge_source.split("private fun initializeCore()", 1)[1].split("private fun loadGameFromVirtualFiles", 1)[0]
assert "if (isGameLoaded) return" not in initialize
assert "val firstLoad = !isGameLoaded" in initialize
assert initialize.count("if (firstLoad)") == 2  # ROM/SRAM and observer are first-load only.
assert initialize.count("LibretroDroid.onSurfaceCreated()") == 1
native_source = (CPP / "libretrodroid.cpp").read_text(encoding="utf-8")
assert "video->updateAspectRatio(getDisplayAspectRatio());" in native_source
surface_rebuild = native_source.split("void LibretroDroid::onSurfaceCreated()", 1)[1].split("void LibretroDroid::onMotionEvent", 1)[0]
assert "hasCurrentGeometry ? currentWidth : system_av_info.geometry.base_width" in surface_rebuild
assert "hasCurrentGeometry ? currentHeight : system_av_info.geometry.base_height" in surface_rebuild
rotation_update = native_source.split("if (video && Environment::getInstance().isScreenRotationUpdated())", 1)[1].split("\n}", 1)[0]
assert "dirtyVideo = true;" in rotation_update
print("PASS: all seven Video presentation mutations invalidate duplicate-frame rendering", flush=True)
print("PASS: EGL recreation and effective-aspect publication source contracts", flush=True)
print("PASS: shader changes wait for a real frame and preserve the hardware core framebuffer", flush=True)

with tempfile.TemporaryDirectory(prefix="emuui-renderer-test-") as temporary:
    work = Path(temporary)
    (work / "android").mkdir()
    # videolayout.cpp only uses disabled LOGD macros; no Android calls are mocked.
    (work / "android/log.h").write_text("#pragma once\n", encoding="utf-8")
    binary = work / "renderregion_test"
    subprocess.run([
        os.environ.get("CXX", "c++"), "-std=c++17", "-Wall", "-Wextra", "-pedantic",
        "-I", str(CPP), "-I", str(work),
        str(ROOT / "tests/renderregion_test.cpp"),
        str(CPP / "renderregion.cpp"), str(CPP / "videolayout.cpp"),
        str(CPP / "shadermanager.cpp"), str(CPP / "utils/rect.cpp"),
        "-o", str(binary),
    ], check=True)
    subprocess.run([str(binary)], check=True)

subprocess.run([sys.executable, str(ROOT / "tests/run_host_input_tests.py")], check=True)
