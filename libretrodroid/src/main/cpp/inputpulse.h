#ifndef LIBRETRODROID_INPUTPULSE_H
#define LIBRETRODROID_INPUTPULSE_H

#include <unordered_map>

namespace libretrodroid {

// Owned by the emulation thread. Only a completed retro_run advances a pulse,
// never wall-clock time, input polls, duplicate video frames or GL draws.
class InputPulses {
public:
    void enqueue(int key) {
        auto& pulse = pulses.try_emplace(key, Pulse{}).first->second;
        // Bound queued activations during a stalled core. Further activations
        // coalesce at this limit rather than overflowing or growing forever.
        if (pulse.pending < MAX_PENDING) ++pulse.pending;
    }

    bool isPressed(int key, bool ordinarilyPressed = false) const {
        const auto entry = pulses.find(key);
        return ordinarilyPressed || (entry != pulses.end() && entry->second.downFrames > 0);
    }

    void afterEmulatedFrame() {
        for (auto entry = pulses.begin(); entry != pulses.end();) {
            auto& pulse = entry->second;
            if (pulse.downFrames > 0) {
                if (--pulse.downFrames == 0) --pulse.pending;
                ++entry;
            } else if (pulse.pending > 0) {
                // One whole up frame has now elapsed, including when another
                // tap arrived during the gap after the previous queue drained.
                pulse.downFrames = DOWN_FRAMES;
                ++entry;
            } else {
                entry = pulses.erase(entry);
            }
        }
    }

    void cancel(int key) { pulses.erase(key); }
    void clear() { pulses.clear(); }

private:
    static constexpr unsigned DOWN_FRAMES = 2;
    static constexpr unsigned MAX_PENDING = 64;
    struct Pulse {
        unsigned pending = 0;
        unsigned downFrames = DOWN_FRAMES;
    };
    std::unordered_map<int, Pulse> pulses;
};

} // namespace libretrodroid

#endif // LIBRETRODROID_INPUTPULSE_H
