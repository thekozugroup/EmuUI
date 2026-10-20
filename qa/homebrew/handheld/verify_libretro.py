#!/usr/bin/env python3
"""Run a generated fixture against a supplied HOST libretro core.

This tests the fixture/core API, not Android, LibretroDroid, app audio output,
touch mapping, lifecycle, filesystem durability, or device performance.
No core or ROM is downloaded by this script. Original CC0-1.0 source.
"""
import argparse
import ctypes as C
import hashlib
import json
from pathlib import Path
import struct
import zlib


class GameInfo(C.Structure):
    _fields_ = [('path', C.c_char_p), ('data', C.c_void_p), ('size', C.c_size_t), ('meta', C.c_char_p)]


class Variable(C.Structure):
    _fields_ = [('key', C.c_char_p), ('value', C.c_char_p)]


def write_host_png(path, frame, size, pixel_format):
    """Encode an unmodified core framebuffer; this is never an app screenshot."""
    width, height = size
    pixels = bytearray()
    if pixel_format == 1:  # XRGB8888, host little endian.
        for offset in range(0, len(frame), 4):
            pixels.extend((frame[offset + 2], frame[offset + 1], frame[offset]))
    else:
        for (value,) in struct.iter_unpack('<H', frame):
            if pixel_format == 2:  # RGB565.
                red, green, blue = (value >> 11) & 31, (value >> 5) & 63, value & 31
                pixels.extend((red * 255 // 31, green * 255 // 63, blue * 255 // 31))
            else:  # 0RGB1555.
                pixels.extend((((value >> 10) & 31) * 255 // 31,
                               ((value >> 5) & 31) * 255 // 31, (value & 31) * 255 // 31))
    scanlines = b''.join(b'\0' + pixels[y * width * 3:(y + 1) * width * 3] for y in range(height))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

    path.write_bytes(b'\x89PNG\r\n\x1a\n' +
                     chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)) +
                     chunk(b'IDAT', zlib.compress(scanlines)) + chunk(b'IEND', b''))


class Core:
    def __init__(self, path, system_dir):
        self.lib = C.CDLL(str(Path(path).resolve()))
        self.buttons = set()
        self.frame = b''
        self.size = None
        self.pixel_format = 0
        self.video_calls = 0
        self.audio_frames = 0
        self.audio_nonzero = 0
        self.directory = str(Path(system_dir).resolve()).encode()
        self.callbacks = []
        self.values = []
        self.options = {
            b'gambatte_gb_bootloader': b'disabled',
            b'gambatte_gb_colorization': b'disabled',
            b'gambatte_gbc_color_correction': b'disabled',
            b'mgba_use_bios': b'OFF',
            b'mgba_skip_bios': b'ON',
        }
        self._callback('environment', C.CFUNCTYPE(C.c_bool, C.c_uint, C.c_void_p), self.environment)
        self._callback('video_refresh', C.CFUNCTYPE(None, C.c_void_p, C.c_uint, C.c_uint, C.c_size_t), self.video)
        self._callback('audio_sample', C.CFUNCTYPE(None, C.c_int16, C.c_int16), self.audio)
        self._callback('audio_sample_batch', C.CFUNCTYPE(C.c_size_t, C.POINTER(C.c_int16), C.c_size_t), self.audio_batch)
        self._callback('input_poll', C.CFUNCTYPE(None), lambda: None)
        self._callback('input_state', C.CFUNCTYPE(C.c_int16, C.c_uint, C.c_uint, C.c_uint, C.c_uint), self.input)
        self.lib.retro_load_game.argtypes = [C.POINTER(GameInfo)]
        self.lib.retro_load_game.restype = C.c_bool
        self.lib.retro_serialize_size.restype = C.c_size_t
        self.lib.retro_serialize.argtypes = [C.c_void_p, C.c_size_t]
        self.lib.retro_serialize.restype = C.c_bool
        self.lib.retro_unserialize.argtypes = [C.c_void_p, C.c_size_t]
        self.lib.retro_unserialize.restype = C.c_bool
        self.lib.retro_get_memory_size.argtypes = [C.c_uint]
        self.lib.retro_get_memory_size.restype = C.c_size_t
        self.lib.retro_get_memory_data.argtypes = [C.c_uint]
        self.lib.retro_get_memory_data.restype = C.c_void_p
        self.lib.retro_init()

    def _callback(self, name, signature, fn):
        callback = signature(fn)
        self.callbacks.append(callback)
        getattr(self.lib, 'retro_set_' + name)(callback)

    def environment(self, command, data):
        command &= 0xFFFF
        if command == 10:  # SET_PIXEL_FORMAT.
            self.pixel_format = C.cast(data, C.POINTER(C.c_uint))[0]
            return True
        if command in (9, 30, 31):  # Directories; the QA directory contains no BIOS.
            C.cast(data, C.POINTER(C.c_char_p))[0] = self.directory
            return True
        if command == 15:  # GET_VARIABLE.
            variable = C.cast(data, C.POINTER(Variable)).contents
            value = self.options.get(variable.key)
            if value is None:
                return False
            self.values.append(value)
            variable.value = value
            return True
        if command in (3, 17):
            C.cast(data, C.POINTER(C.c_bool))[0] = command == 3
            return True
        if command in (39, 52):
            C.cast(data, C.POINTER(C.c_uint))[0] = 0
            return True
        if command in (1, 11, 16, 18, 32, 35, 36, 37, 42, 53, 54):
            return True
        return False

    def input(self, port, device, index, button):
        if port != 0 or device != 1:
            return 0
        if button == 256:
            return sum(1 << button for button in self.buttons)
        return int(button in self.buttons)

    def video(self, data, width, height, pitch):
        if not data:
            return
        self.video_calls += 1
        self.size = [width, height]
        bytes_per_pixel = 4 if self.pixel_format == 1 else 2
        self.frame = b''.join(C.string_at(data + y * pitch, width * bytes_per_pixel) for y in range(height))

    def audio(self, left, right):
        self.audio_frames += 1
        self.audio_nonzero += int(left != 0 or right != 0)

    def audio_batch(self, data, frames):
        self.audio_frames += frames
        self.audio_nonzero += sum(data[i] != 0 for i in range(frames * 2))
        return frames

    def load(self, path):
        self.rom = C.create_string_buffer(Path(path).read_bytes())
        self.rom_path = str(Path(path).resolve()).encode()
        game = GameInfo(self.rom_path, C.cast(self.rom, C.c_void_p), len(self.rom) - 1, None)
        assert self.lib.retro_load_game(C.byref(game)), 'Core refused fixture'

    def run(self, frames, *buttons):
        self.buttons = set(buttons)
        for _ in range(frames):
            self.lib.retro_run()

    def memory(self, memory_id):
        size = self.lib.retro_get_memory_size(memory_id)
        pointer = self.lib.retro_get_memory_data(memory_id)
        return C.string_at(pointer, size) if pointer and size else b''

    def restore_memory(self, memory_id, data):
        # mGBA exposes the maximum save buffer until hardware autodetection.
        # Copy the persisted prefix into that allocation before executing guest code.
        assert self.lib.retro_get_memory_size(memory_id) >= len(data)
        C.memmove(self.lib.retro_get_memory_data(memory_id), data, len(data))

    def save(self):
        size = self.lib.retro_serialize_size()
        assert size > 0, 'Core has no save state'
        buffer = C.create_string_buffer(size)
        assert self.lib.retro_serialize(buffer, size), 'Core serialization failed'
        return buffer.raw

    def restore(self, state):
        buffer = C.create_string_buffer(state)
        assert self.lib.retro_unserialize(buffer, len(state)), 'Core unserialization failed'

    def close(self):
        self.lib.retro_unload_game()
        self.lib.retro_deinit()


def verify(core_path, rom_path, work_dir):
    work_dir = Path(work_dir)
    work_dir.mkdir(parents=True, exist_ok=True)
    core = Core(core_path, work_dir)
    try:
        core.load(rom_path)
        core.run(180)
        assert core.size in ([160, 144], [240, 160]), f'Unexpected dimensions {core.size}'
        assert len(set(core.frame)) > 1, 'No nonuniform rendered picture'
        assert core.audio_nonzero > 0, 'No nonzero audio samples'
        png_path = work_dir / (Path(rom_path).name + '.host-framebuffer.png')
        write_host_png(png_path, core.frame, core.size, core.pixel_format)
        for button, direction in [(7, 'Right'), (5, 'Down'), (6, 'Left'), (4, 'Up')]:
            before = core.frame
            core.run(12, button)
            assert core.frame != before, f'{direction} input did not change picture'
        moved = core.frame
        core.run(8, 8)  # A.
        assert core.frame != moved, 'A input did not change picture'
        core.run(3)
        ram = core.memory(0)
        assert len(ram) >= 2 and ram[:2] == b'\xA5\x01', f'Battery RAM marker/count wrong: {ram[:8].hex()}'
        state = core.save()
        state_path = work_dir / (Path(rom_path).name + '.state')
        state_path.write_bytes(state)
        saved_frame = core.frame
        core.run(15, 6)  # Left.
        assert core.frame != saved_frame, 'Mutation did not change picture'
        core.restore(state_path.read_bytes())
        core.run(2)
        assert core.frame == saved_frame, 'Restored visible player did not match saved frame'
        # No retro_run while paused: the core remains serialized-identical.
        before = core.save()
        after = core.save()
        assert before == after, 'State changed without retro_run'
        core.run(5)
        assert core.save() != before, 'Resume did not advance core state'
        saved_ram = core.memory(0)
        sram_path = work_dir / (Path(rom_path).name + '.srm')
        sram_path.write_bytes(saved_ram)
        evidence = {
            'scope': 'host-libretro-fixture-only',
            'core': str(Path(core_path).resolve()),
            'core_sha256': hashlib.sha256(Path(core_path).read_bytes()).hexdigest(),
            'rom': Path(rom_path).name,
            'rom_sha256': hashlib.sha256(Path(rom_path).read_bytes()).hexdigest(),
            'dimensions': core.size,
            'video_callbacks': core.video_calls,
            'audio_frames': core.audio_frames,
            'nonzero_audio_samples_or_frames': core.audio_nonzero,
            'save_state_bytes': len(state),
            'battery_ram_bytes': len(saved_ram),
            'host_framebuffer_png': png_path.name,
            'host_framebuffer_png_sha256': hashlib.sha256(png_path.read_bytes()).hexdigest(),
            'passed': ['load', 'nonuniform-video', 'four-direction-input', 'a-input', 'nonzero-audio',
                       'save-load-visible-equality', 'paused-state-stable', 'resume-advances-state'],
        }
    finally:
        core.close()
    # Reloading the core with its battery RAM proves the fixture does not reset it.
    core = Core(core_path, work_dir)
    try:
        core.load(rom_path)
        core.restore_memory(0, sram_path.read_bytes())
        core.run(10)
        assert core.memory(0)[:2] == b'\xA5\x01', 'Fresh session did not retain battery counter'
        evidence['passed'].append('battery-ram-file-reload')
    finally:
        core.close()
    return evidence


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--core', required=True, type=Path)
    parser.add_argument('--rom', required=True, type=Path)
    parser.add_argument('--work-dir', required=True, type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    evidence = verify(args.core, args.rom, args.work_dir)
    text = json.dumps(evidence, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text)
    print(text)


if __name__ == '__main__':
    main()
