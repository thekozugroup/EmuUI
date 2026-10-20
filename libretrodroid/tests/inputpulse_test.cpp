#include "input.h"
#include <android/input.h>
#include <android/keycodes.h>
#include <libretro.h>
#include <cassert>
#include <iostream>
#include <limits>
#include <memory>
#include <vector>

using libretrodroid::Input;
using libretrodroid::InputPulses;

static bool pressed(Input& input, unsigned id = RETRO_DEVICE_ID_JOYPAD_A) {
    return input.getInputState(0, RETRO_DEVICE_JOYPAD, 0, id) != 0;
}

int main() {
    {
        Input input;
        // Android/JNI controller indices must not address outside the four
        // libretro pads. Invalid writes leave every valid pad unchanged.
        for (unsigned port : {4u, std::numeric_limits<unsigned>::max()}) {
            input.onKeyEvent(port, AKEY_EVENT_ACTION_DOWN, AKEYCODE_BUTTON_A);
            input.enqueueAccessibleTap(port, AKEYCODE_BUTTON_A);
            input.cancelAccessibleTaps(port, -1);
            assert(input.getInputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A) == 0);
            assert(input.getInputState(port, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X) == 0);
            assert(input.getInputState(port, RETRO_DEVICE_POINTER, 0, RETRO_DEVICE_ID_POINTER_PRESSED) == 0);
        }
        for (int port : {-1, 4, std::numeric_limits<int>::min(), std::numeric_limits<int>::max()}) {
            for (int source : {Input::MOTION_SOURCE_DPAD, Input::MOTION_SOURCE_ANALOG_LEFT,
                               Input::MOTION_SOURCE_ANALOG_RIGHT, Input::MOTION_SOURCE_POINTER}) {
                input.onMotionEvent(port, source, 1, 1);
            }
        }
        for (unsigned port = 0; port < 4; ++port) {
            assert(input.getInputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A) == 0);
            assert(input.getInputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT) == 0);
            assert(input.getInputState(port, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X) == 0);
            assert(input.getInputState(port, RETRO_DEVICE_POINTER, 0, RETRO_DEVICE_ID_POINTER_PRESSED) == 0);
        }
        input.onKeyEvent(3, AKEY_EVENT_ACTION_DOWN, AKEYCODE_BUTTON_A);
        input.onMotionEvent(3, Input::MOTION_SOURCE_ANALOG_LEFT, 1, -1);
        assert(input.getInputState(3, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A) == 1);
        assert(input.getInputState(3, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X) == Input::MAX_RANGE_MOTION);
        assert(input.getInputState(3, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y) == -Input::MAX_RANGE_MOTION);
    }
    {
        Input input;
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        // All taps may enter before the first frame. Repeated polling does not
        // shorten them, and each activation gets a real released frame.
        for (int frame = 0; frame < 9; ++frame) {
            for (int poll = 0; poll < 20; ++poll) assert(pressed(input) == (frame % 3 < 2));
            input.afterEmulatedFrame();
        }
        assert(!pressed(input));
    }
    {
        Input input;
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.afterEmulatedFrame();
        input.afterEmulatedFrame();
        assert(!pressed(input));
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        assert(!pressed(input)); // A new tap cannot skip the still-unobserved gap.
        input.afterEmulatedFrame();
        assert(pressed(input));
    }
    {
        Input input;
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.onKeyEvent(0, AKEY_EVENT_ACTION_DOWN, AKEYCODE_BUTTON_A);
        for (int frame = 0; frame < 8; ++frame) {
            input.afterEmulatedFrame();
            assert(pressed(input)); // Pulse completion cannot lift a newer hold.
        }
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.cancelAccessibleTaps(0, AKEYCODE_BUTTON_A);
        assert(pressed(input));
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.cancelAccessibleTaps(0, -1);
        assert(pressed(input));
        input.onKeyEvent(0, AKEY_EVENT_ACTION_UP, AKEYCODE_BUTTON_A);
        assert(!pressed(input));
    }
    {
        Input input;
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_B);
        input.cancelAccessibleTaps(0, AKEYCODE_BUTTON_A);
        assert(!pressed(input));
        assert(pressed(input, RETRO_DEVICE_ID_JOYPAD_B));
        input.cancelAccessibleTaps(0, -1);
        assert(!pressed(input, RETRO_DEVICE_ID_JOYPAD_B));
        for (int frame = 0; frame < 8; ++frame) input.afterEmulatedFrame();
        assert(!pressed(input));
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        assert(pressed(input)); // Cancellation permits a clean new activation.
        auto resumedInput = std::make_unique<Input>(); // Native resume creates fresh Input.
        assert(!pressed(*resumedInput));
    }
    {
        Input input;
        input.enqueueAccessibleTap(0, 99999);
        input.enqueueAccessibleTap(0, -1);
        input.enqueueAccessibleTap(4, AKEYCODE_BUTTON_A);
        input.enqueueAccessibleTap(static_cast<unsigned>(-1), AKEYCODE_BUTTON_A);
        assert(!pressed(input));
        input.enqueueAccessibleTap(0, AKEYCODE_BUTTON_A);
        input.cancelAccessibleTaps(0, 99999);
        input.cancelAccessibleTaps(0, -99);
        input.cancelAccessibleTaps(4, -1);
        assert(pressed(input));
        input.enqueueAccessibleTap(0, AKEYCODE_DPAD_UP_RIGHT);
        assert(pressed(input, RETRO_DEVICE_ID_JOYPAD_UP));
        assert(pressed(input, RETRO_DEVICE_ID_JOYPAD_RIGHT));
    }
    {
        InputPulses pulses;
        for (int tap = 0; tap < 10000; ++tap) pulses.enqueue(1);
        int downFrames = 0;
        int downEdges = 0;
        bool previous = false;
        for (int frame = 0; frame < 300; ++frame) {
            const bool down = pulses.isPressed(1);
            downFrames += down;
            downEdges += down && !previous;
            previous = down;
            pulses.afterEmulatedFrame();
        }
        assert(downFrames == 128 && downEdges == 64);
        assert(!pulses.isPressed(1));
    }
    {
        InputPulses pulses;
        for (int tap = 0; tap < 4; ++tap) pulses.enqueue(1);
        std::vector<bool> observations;
        // Fast-forward/catch-up may execute several core runs per GL draw.
        // A zero-run draw cannot consume any part of a pulse.
        for (int runs : {0, 4, 2, 4, 2}) {
            for (int run = 0; run < runs; ++run) {
                observations.push_back(pulses.isPressed(1));
                pulses.afterEmulatedFrame();
            }
        }
        assert(observations.size() == 12);
        for (size_t frame = 0; frame < observations.size(); ++frame) {
            assert(observations[frame] == (frame % 3 < 2));
        }
    }
    std::cout << "PASS: native input port bounds, pulse timing, queue bounds, key validation, holds and cancellation\n";
}
