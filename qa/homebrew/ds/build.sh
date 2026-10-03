#!/usr/bin/env bash
# Original build wrapper. SPDX-License-Identifier: CC0-1.0
set -euo pipefail
: "${DEVKITPRO:?Set DEVKITPRO to the installed official devkitPro SDK root}"
: "${DEVKITARM:=$DEVKITPRO/devkitARM}"
source_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
out="${1:?Pass an output directory outside the public repository}"
mkdir -p "$out"
out="$(cd -- "$out" && pwd)"
repo_root="$(cd -- "$source_dir/../../.." && pwd)"
case "$out/" in
    "$repo_root/"*) echo "Keep compiled QA binaries outside the public repository" >&2; exit 2 ;;
esac
export PATH="$DEVKITARM/bin:$DEVKITPRO/tools/bin:$PATH"
arm-none-eabi-gcc -std=gnu11 -O2 -Wall -Wextra -Werror \
    -ffunction-sections -fdata-sections -march=armv5te -mtune=arm946e-s \
    -DARM9 -D__NDS__ -I"$DEVKITPRO/libnds/include" -I"$DEVKITPRO/calico/include" \
    -c "$source_dir/main.c" -o "$out/EmuUI_DS_QA.o"
arm-none-eabi-gcc -march=armv5te -mtune=arm946e-s \
    -specs="$DEVKITPRO/calico/share/ds9.specs" \
    -L"$DEVKITPRO/libnds/lib" -L"$DEVKITPRO/calico/lib" \
    -Wl,-Map,"$out/EmuUI_DS_QA.map" "$out/EmuUI_DS_QA.o" \
    -lnds9 -lcalico_ds9 -o "$out/EmuUI_DS_QA.elf"
# An original blank header field with a generated CRC adjustment, never Nintendo artwork.
python3 "$source_dir/header.py" logo "$out/homebrew-logo.bin"
ndstool -c "$out/EmuUI_DS_Homebrew_QA.nds" -9 "$out/EmuUI_DS_QA.elf" \
    -7 "$DEVKITPRO/calico/bin/ds7_maine.elf" -o "$out/homebrew-logo.bin" \
    -g "####" 00 "EMUUI DS QA"
python3 "$source_dir/header.py" validate "$out/EmuUI_DS_Homebrew_QA.nds" "$source_dir/main.c"
