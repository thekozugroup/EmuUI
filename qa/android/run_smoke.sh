#!/usr/bin/env bash
# Run inside ONE persistent shell: emulator/ADB may be isolated per exec invocation.
set -euo pipefail
usage() {
    cat <<'HELP'
Usage: run_smoke.sh --apk APP.apk [--test-apk TEST.apk] [--avd NAME]
                    [--serial emulator-5554] [--package ID] [--output DIR]
                    [--boot-timeout SECONDS] [--keep-emulator]
Requires ANDROID_HOME (installed SDK with accepted terms), Python 3 and timeout.
Uses only an emulator; never clears app data or grants runtime permissions.
--avd launches a headless software emulator in this same shell. Without it,
connect to --serial already running in this shell's process/network namespace.
The original QA cartridge is generated and staged in /sdcard/EmuUI-QA. Import it
through EmuUI's folder picker for separate gameplay testing. Staging is not import.
HELP
}
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APK=; TEST_APK=; AVD=; SERIAL=emulator-5554; PACKAGE=
OUTPUT="$ROOT/.qa-output/$(date -u +%Y%m%dT%H%M%SZ)"; BOOT_TIMEOUT=1800; KEEP=0
while (($#)); do
    case "$1" in
        --apk) APK="$2"; shift 2;;
        --test-apk) TEST_APK="$2"; shift 2;;
        --avd) AVD="$2"; shift 2;;
        --serial) SERIAL="$2"; shift 2;;
        --package) PACKAGE="$2"; shift 2;;
        --output) OUTPUT="$2"; shift 2;;
        --boot-timeout) BOOT_TIMEOUT="$2"; shift 2;;
        --keep-emulator) KEEP=1; shift;;
        -h|--help) usage; exit 0;;
        *) echo "Unknown argument: $1" >&2; usage; exit 2;;
    esac
done
[[ -n "$APK" && -f "$APK" ]] || { usage; exit 2; }
[[ "$SERIAL" =~ ^emulator-[0-9]+$ ]] || { echo 'Refusing non-emulator device' >&2; exit 2; }
[[ "$BOOT_TIMEOUT" =~ ^[0-9]+$ ]] || exit 2
: "${ANDROID_HOME:?Set ANDROID_HOME to the authorized installed SDK}"
ADB="$ANDROID_HOME/platform-tools/adb"; EMULATOR="$ANDROID_HOME/emulator/emulator"
if [[ -z "$PACKAGE" ]]; then
    AAPT=$(find "$ANDROID_HOME/build-tools" -name aapt -type f | sort -V | tail -1)
    [[ -n "$AAPT" ]] || { echo 'Supply --package or install build-tools/aapt' >&2; exit 2; }
    PACKAGE=$("$AAPT" dump badging "$APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
    [[ -n "$PACKAGE" ]] || { echo 'Could not identify APK package' >&2; exit 2; }
fi
[[ -x "$ADB" ]] || { echo 'ADB is missing' >&2; exit 2; }
mkdir -p "$OUTPUT/test-content"
OUTPUT="$(cd "$OUTPUT" && pwd)"
# Preserve the configured AVD lookup before relocating writable per-run user data.
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$OUTPUT/android-home}"
export HOME="${EMUUI_QA_HOME:-$OUTPUT/home}"; mkdir -p "$HOME" "$ANDROID_USER_HOME"
EMU_PID=; LOG_PID=
cleanup() {
    status=$?
    if [[ -n "$LOG_PID" ]]; then kill "$LOG_PID" 2>/dev/null || true; fi
    if [[ -n "$EMU_PID" && "$KEEP" == 0 ]]; then
        "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
        kill "$EMU_PID" 2>/dev/null || true
    fi
    printf '{"scriptExitCode":%s,"emulator":"%s","gameplayTested":false}\n' "$status" "$SERIAL" > "$OUTPUT/status.json"
    echo "Evidence: $OUTPUT"
}
trap cleanup EXIT
if [[ -n "$AVD" ]]; then
    port="${SERIAL#emulator-}"
    "$EMULATOR" -avd "$AVD" -port "$port" -accel off -no-window -no-audio \
        -no-snapshot -no-boot-anim -gpu swiftshader -memory 1536 -cores 2 \
        -skin 1280x720 > "$OUTPUT/emulator.log" 2>&1 < /dev/null &
    EMU_PID=$!
fi
"$ADB" start-server
start=$SECONDS
while :; do
    boot=$(timeout 20 "$ADB" -s "$SERIAL" shell -n getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
    [[ "$boot" == 1 ]] && break
    if [[ -n "$EMU_PID" ]] && ! kill -0 "$EMU_PID" 2>/dev/null; then echo 'Emulator exited before boot' >&2; exit 1; fi
    (( SECONDS - start < BOOT_TIMEOUT )) || { echo 'Android boot deadline exceeded' >&2; exit 1; }
    sleep 5
done
"$ADB" -s "$SERIAL" shell -n input keyevent KEYCODE_WAKEUP
"$ADB" -s "$SERIAL" shell -n wm dismiss-keyguard
"$ADB" -s "$SERIAL" shell -n getprop > "$OUTPUT/device-properties.txt"
"$ADB" -s "$SERIAL" logcat -v threadtime < /dev/null > "$OUTPUT/logcat.txt" & LOG_PID=$!
"$ADB" -s "$SERIAL" install -r -t "$APK" | tee "$OUTPUT/app-install.txt"
# No ROM binary is tracked in the public source tree.
cp "$ROOT/qa/homebrew/build_qa_rom.py" "$OUTPUT/test-content/"
python3 "$OUTPUT/test-content/build_qa_rom.py" | tee "$OUTPUT/homebrew-build.txt"
"$ADB" -s "$SERIAL" shell -n mkdir -p /sdcard/EmuUI-QA
"$ADB" -s "$SERIAL" push "$OUTPUT/test-content/EmuUI_QA.nes" /sdcard/EmuUI-QA/ | tee "$OUTPUT/homebrew-stage.txt"
"$ADB" -s "$SERIAL" shell -n am start -W -n "$PACKAGE/com.swordfish.lemuroid.app.mobile.feature.main.MainActivity" | tee "$OUTPUT/app-launch.txt"
"$ADB" -s "$SERIAL" shell -n uiautomator dump /sdcard/emuui-ui.xml >/dev/null
"$ADB" -s "$SERIAL" pull /sdcard/emuui-ui.xml "$OUTPUT/launch-ui.xml" >/dev/null
"$ADB" -s "$SERIAL" exec-out screencap -p < /dev/null > "$OUTPUT/launch.png"
if [[ -n "$TEST_APK" ]]; then
    "$ADB" -s "$SERIAL" install -r -t "$TEST_APK" | tee "$OUTPUT/test-install.txt"
    "$ADB" -s "$SERIAL" shell -n am instrument -w -r \
        -e class com.swordfish.lemuroid.app.mobile.feature.main.MainActivitySmokeTest,com.swordfish.lemuroid.app.mobile.feature.main.InjectedFoldLayoutTest \
        "$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUTPUT/instrumentation.txt"
    if grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$OUTPUT/instrumentation.txt" || \
       ! grep -Eq 'OK \([0-9]+ tests?\)' "$OUTPUT/instrumentation.txt"; then
        echo 'Instrumentation did not pass; see evidence' >&2
        exit 1
    fi
fi
"$ADB" -s "$SERIAL" shell -n dumpsys activity activities > "$OUTPUT/activity-state.txt"
"$ADB" -s "$SERIAL" shell -n dumpsys window windows > "$OUTPUT/window-state.txt"
"$ADB" -s "$SERIAL" exec-out screencap -p < /dev/null > "$OUTPUT/final.png"
echo 'Startup/instrumentation collection finished. Native gameplay remains a separate check.'
