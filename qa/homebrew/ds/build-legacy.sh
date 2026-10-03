#!/usr/bin/env bash
# Original build wrapper. SPDX-License-Identifier: CC0-1.0
set -euo pipefail
: "${DEVKITPRO:?Set DEVKITPRO to the installed official devkitPro SDK root}"
: "${DEVKITARM:=$DEVKITPRO/devkitARM}"
: "${LIBNDS_LEGACY:?Set LIBNDS_LEGACY to an extracted official libnds 1.8.3 directory}"
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
    -DARM9 -D__NDS__ -I"$LIBNDS_LEGACY/include" \
    -c "$source_dir/main.c" -o "$out/EmuUI_DS_QA.o"
arm-none-eabi-gcc -march=armv5te -mtune=arm946e-s -specs=ds_arm9.specs \
    -L"$LIBNDS_LEGACY/lib" -Wl,-Map,"$out/EmuUI_DS_QA.map" \
    "$out/EmuUI_DS_QA.o" -lnds9 -o "$out/EmuUI_DS_QA.elf"
arm-none-eabi-gcc -std=gnu11 -O2 -Wall -Wextra -Werror \
    -ffunction-sections -fdata-sections -mcpu=arm7tdmi -mthumb -mthumb-interwork \
    -DARM7 -D__NDS__ -I"$LIBNDS_LEGACY/include" \
    -c "$source_dir/arm7-legacy.c" -o "$out/EmuUI_DS_ARM7.o"
arm-none-eabi-gcc -mcpu=arm7tdmi -mthumb -mthumb-interwork -specs=ds_arm7.specs \
    -L"$LIBNDS_LEGACY/lib" -Wl,-Map,"$out/EmuUI_DS_ARM7.map" \
    "$out/EmuUI_DS_ARM7.o" -lnds7 -o "$out/EmuUI_DS_ARM7.elf"
python3 "$source_dir/header.py" logo "$out/homebrew-logo.bin"
ndstool -c "$out/EmuUI_DS_Legacy_QA.nds" -9 "$out/EmuUI_DS_QA.elf" \
    -7 "$out/EmuUI_DS_ARM7.elf" -o "$out/homebrew-logo.bin" \
    -g "####" 00 "EMUUI DS QA"
python3 "$source_dir/header.py" validate "$out/EmuUI_DS_Legacy_QA.nds" \
    "$source_dir/main.c" --runtime legacy --arm7-source "$source_dir/arm7-legacy.c"
