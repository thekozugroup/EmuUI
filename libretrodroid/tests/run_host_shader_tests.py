#!/usr/bin/env python3
"""Compile/link the actual built-in shader chains using an installed surfaceless EGL.

This optional check needs libEGL.so.1 with EGL_MESA_platform_surfaceless. It does
not download or install a driver and is not a substitute for Android GPU QA.
"""
import ctypes as C
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CPP = ROOT / "src/main/cpp"
os.environ.setdefault("MESA_SHADER_CACHE_DISABLE", "true")


def egl_function(egl, name, result, *arguments):
    function = getattr(egl, name)
    function.restype = result
    function.argtypes = arguments
    return function


with tempfile.TemporaryDirectory(prefix="emuui-shader-test-") as directory:
    work = Path(directory)
    generator = work / "generate.cpp"
    generator.write_text(r'''
#include <fstream>
#include <string>
#include "renderregion.h"
#include "shadermanager.h"
int main(int argc, char** argv) {
    if (argc != 2) return 1;
    using namespace libretrodroid;
    for (int type = 0; type <= 6; ++type) {
        const auto chain = ShaderManager::getShader({static_cast<ShaderManager::Type>(type), {}});
        for (size_t pass = 0; pass < chain.passes.size(); ++pass) {
            const auto& item = chain.passes[pass];
            const std::string base = std::string(argv[1]) + "/" + std::to_string(type) + "-" + std::to_string(pass);
            std::ofstream(base + ".vert") << item.vertex;
            std::ofstream(base + ".frag") << (pass + 1 == chain.passes.size() ? withRenderRegionSampling(item.fragment) : item.fragment);
        }
    }
}
''', encoding="utf-8")
    binary = work / "generate"
    subprocess.run([
        os.environ.get("CXX", "c++"), "-std=c++17", "-I", str(CPP),
        str(generator), str(CPP / "renderregion.cpp"),
        str(CPP / "shadermanager.cpp"), str(CPP / "utils/rect.cpp"),
        "-o", str(binary),
    ], check=True)
    subprocess.run([str(binary), str(work)], check=True)

    egl = C.CDLL("libEGL.so.1")
    get_proc = egl_function(egl, "eglGetProcAddress", C.c_void_p, C.c_char_p)
    get_display_address = get_proc(b"eglGetPlatformDisplayEXT")
    if not get_display_address:
        raise RuntimeError("Installed EGL lacks surfaceless display support")
    get_display = C.CFUNCTYPE(C.c_void_p, C.c_uint, C.c_void_p, C.c_void_p)(get_display_address)
    display = get_display(0x31DD, None, None)  # EGL_PLATFORM_SURFACELESS_MESA
    initialize = egl_function(egl, "eglInitialize", C.c_uint, C.c_void_p, C.POINTER(C.c_int), C.POINTER(C.c_int))
    major, minor = C.c_int(), C.c_int()
    if not initialize(display, C.byref(major), C.byref(minor)):
        raise RuntimeError("Could not initialize installed surfaceless EGL")

    bind_api = egl_function(egl, "eglBindAPI", C.c_uint, C.c_uint)
    assert bind_api(0x30A0)  # EGL_OPENGL_ES_API
    choose = egl_function(egl, "eglChooseConfig", C.c_uint, C.c_void_p, C.POINTER(C.c_int), C.POINTER(C.c_void_p), C.c_int, C.POINTER(C.c_int))
    attributes = (C.c_int * 9)(0x3033, 1, 0x3040, 4, 0x3024, 8, 0x3023, 8, 0x3038)
    config, count = C.c_void_p(), C.c_int()
    assert choose(display, attributes, C.byref(config), 1, C.byref(count)) and count.value
    create_surface = egl_function(egl, "eglCreatePbufferSurface", C.c_void_p, C.c_void_p, C.c_void_p, C.POINTER(C.c_int))
    surface = create_surface(display, config, (C.c_int * 5)(0x3057, 16, 0x3056, 16, 0x3038))
    create_context = egl_function(egl, "eglCreateContext", C.c_void_p, C.c_void_p, C.c_void_p, C.c_void_p, C.POINTER(C.c_int))
    context = create_context(display, config, None, (C.c_int * 3)(0x3098, 2, 0x3038))
    make_current = egl_function(egl, "eglMakeCurrent", C.c_uint, C.c_void_p, C.c_void_p, C.c_void_p, C.c_void_p)
    assert surface and context and make_current(display, surface, surface, context)

    def gl(name, result, *arguments):
        address = get_proc(name.encode())
        if not address:
            raise RuntimeError("Missing installed GL entry point: " + name)
        return C.CFUNCTYPE(result, *arguments)(address)

    create_shader = gl("glCreateShader", C.c_uint, C.c_uint)
    shader_source = gl("glShaderSource", None, C.c_uint, C.c_int, C.POINTER(C.c_char_p), C.POINTER(C.c_int))
    compile_shader = gl("glCompileShader", None, C.c_uint)
    get_shader = gl("glGetShaderiv", None, C.c_uint, C.c_uint, C.POINTER(C.c_int))
    shader_log = gl("glGetShaderInfoLog", None, C.c_uint, C.c_int, C.POINTER(C.c_int), C.c_char_p)
    delete_shader = gl("glDeleteShader", None, C.c_uint)
    create_program = gl("glCreateProgram", C.c_uint)
    attach_shader = gl("glAttachShader", None, C.c_uint, C.c_uint)
    link_program = gl("glLinkProgram", None, C.c_uint)
    get_program = gl("glGetProgramiv", None, C.c_uint, C.c_uint, C.POINTER(C.c_int))
    program_log = gl("glGetProgramInfoLog", None, C.c_uint, C.c_int, C.POINTER(C.c_int), C.c_char_p)
    delete_program = gl("glDeleteProgram", None, C.c_uint)
    get_string = gl("glGetString", C.c_char_p, C.c_uint)

    programs = 0
    try:
        for vertex in sorted(work.glob("*.vert")):
            shaders = []
            for file, kind in [(vertex, 0x8B31), (vertex.with_suffix(".frag"), 0x8B30)]:
                shader = create_shader(kind)
                text = C.c_char_p(file.read_bytes())
                shader_source(shader, 1, C.byref(text), None)
                compile_shader(shader)
                status = C.c_int()
                get_shader(shader, 0x8B81, C.byref(status))
                log = C.create_string_buffer(32768)
                shader_log(shader, len(log), None, log)
                if not status.value:
                    raise AssertionError(f"{file.name} failed GLSL compilation: {log.value.decode()}")
                shaders.append(shader)
            program = create_program()
            for shader in shaders:
                attach_shader(program, shader)
            link_program(program)
            status = C.c_int()
            get_program(program, 0x8B82, C.byref(status))
            log = C.create_string_buffer(32768)
            program_log(program, len(log), None, log)
            if not status.value:
                raise AssertionError(f"{vertex.stem} failed GLSL linking: {log.value.decode()}")
            delete_program(program)
            for shader in shaders:
                delete_shader(shader)
            programs += 1
        print(f"PASS: {programs} shader-pass programs compiled and linked across all seven built-in chains")
        print("Installed GL renderer:", get_string(0x1F01).decode())
        print("Installed GL version:", get_string(0x1F02).decode())
    finally:
        make_current(display, None, None, None)
        egl_function(egl, "eglDestroyContext", C.c_uint, C.c_void_p, C.c_void_p)(display, context)
        egl_function(egl, "eglDestroySurface", C.c_uint, C.c_void_p, C.c_void_p)(display, surface)
        egl_function(egl, "eglTerminate", C.c_uint, C.c_void_p)(display)
