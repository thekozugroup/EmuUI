/* EmuUI original DS QA fixture. SPDX-License-Identifier: CC0-1.0
 * Created for lawful emulator verification; no game, BIOS, or firmware data.
 */
#include <nds.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>

#define WIDTH 256
#define HEIGHT 192
#define COLOR(r, g, b) (RGB15((r), (g), (b)) | BIT(15))

/* Original compact 5x7 glyphs; only the fixture's own UI uses them. */
static const uint8_t glyphs[][7] = {
    {14,17,19,21,25,17,14}, {4,12,4,4,4,4,14},
    {14,17,1,2,4,8,31}, {30,1,1,14,1,1,30},
    {2,6,10,18,31,2,2}, {31,16,16,30,1,1,30},
    {14,16,16,30,17,17,14}, {31,1,2,4,8,8,8},
    {14,17,17,14,17,17,14}, {14,17,17,15,1,1,14},
    {14,17,17,31,17,17,17}, {30,17,17,30,17,17,30},
    {15,16,16,16,16,16,15}, {30,17,17,17,17,17,30},
    {31,16,16,30,16,16,31}, {31,16,16,30,16,16,16},
    {15,16,16,19,17,17,15}, {17,17,17,31,17,17,17},
    {14,4,4,4,4,4,14}, {7,2,2,2,2,18,12},
    {17,18,20,24,20,18,17}, {16,16,16,16,16,16,31},
    {17,27,21,21,17,17,17}, {17,25,21,19,17,17,17},
    {14,17,17,17,17,17,14}, {30,17,17,30,16,16,16},
    {14,17,17,17,21,18,13}, {30,17,17,30,20,18,17},
    {15,16,16,14,1,1,30}, {31,4,4,4,4,4,4},
    {17,17,17,17,17,17,14}, {17,17,17,17,17,10,4},
    {17,17,17,21,21,27,17}, {17,17,10,4,10,17,17},
    {17,17,10,4,4,4,4}, {31,1,2,4,8,16,31},
    {0,4,4,31,4,4,0}, {0,0,0,31,0,0,0},
    {0,4,4,0,4,4,0}, {0,0,31,0,31,0,0},
};

static void rect(uint16_t *pixels, int x, int y, int w, int h, uint16_t color) {
    for (int yy = y; yy < y + h; ++yy) {
        if (yy < 0 || yy >= HEIGHT) continue;
        for (int xx = x; xx < x + w; ++xx) {
            if (xx >= 0 && xx < WIDTH) pixels[yy * WIDTH + xx] = color;
        }
    }
}

static void text(uint16_t *pixels, int x, int y, const char *s, int scale, uint16_t color) {
    for (; *s; ++s, x += 6 * scale) {
        int index = -1;
        if (*s >= '0' && *s <= '9') index = *s - '0';
        if (*s >= 'A' && *s <= 'Z') index = *s - 'A' + 10;
        if (*s == '+') index = 36;
        if (*s == '-') index = 37;
        if (*s == ':') index = 38;
        if (*s == '=') index = 39;
        if (index < 0) continue;
        for (int row = 0; row < 7; ++row) {
            for (int col = 0; col < 5; ++col) {
                if (glyphs[index][row] & (1 << (4 - col)))
                    rect(pixels, x + col * scale, y + row * scale, scale, scale, color);
            }
        }
    }
}

static int clamp(int v, int low, int high) { return v < low ? low : v > high ? high : v; }

static void draw_bottom(uint16_t *pixels, int x, int y, uint32_t touches, bool down) {
    const uint16_t bg = COLOR(3, 7, 7);
    const uint16_t grid = COLOR(7, 13, 13);
    const uint16_t white = COLOR(31, 31, 31);
    char line[40];
    rect(pixels, 0, 0, WIDTH, HEIGHT, bg);
    for (int xx = 0; xx < WIDTH; xx += 32) rect(pixels, xx, 0, 1, HEIGHT, grid);
    for (int yy = 0; yy < HEIGHT; yy += 32) rect(pixels, 0, yy, WIDTH, 1, grid);
    text(pixels, 8, 8, "BOTTOM TOUCH", 2, COLOR(12, 31, 24));
    snprintf(line, sizeof(line), "X %03d Y %03d", x, y);
    text(pixels, 8, 31, line, 2, white);
    snprintf(line, sizeof(line), "TOUCHES %lu", (unsigned long)touches);
    text(pixels, 8, 52, line, 1, white);
    text(pixels, 8, 176, down ? "TOUCH DOWN" : "TOUCH UP", 1, white);
    const uint16_t marker = down ? COLOR(31, 8, 13) : COLOR(31, 27, 5);
    rect(pixels, x - 10, y - 1, 21, 3, marker);
    rect(pixels, x - 1, y - 10, 3, 21, marker);
    rect(pixels, x - 4, y - 4, 9, 1, white);
    rect(pixels, x - 4, y + 4, 9, 1, white);
    rect(pixels, x - 4, y - 4, 1, 9, white);
    rect(pixels, x + 4, y - 4, 1, 9, white);
}

int main(void) {
    powerOn(POWER_ALL_2D);
    videoSetMode(MODE_5_2D);
    videoSetModeSub(MODE_5_2D);
    vramSetBankA(VRAM_A_MAIN_BG);
    vramSetBankC(VRAM_C_SUB_BG);
    lcdMainOnTop();
    int mainBg = bgInit(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    int subBg = bgInitSub(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    uint16_t *top = bgGetGfxPtr(mainBg);
    uint16_t *bottom = bgGetGfxPtr(subBg);
    const uint16_t topColor = COLOR(5, 3, 12);
    const uint16_t white = COLOR(31, 31, 31);
    uint32_t frames = 0, state = 17, touches = 0;
    int x = 128, y = 112;
    bool frozen = false, wasTouch = false;
    char line[40];
    rect(top, 0, 0, WIDTH, HEIGHT, topColor);
    text(top, 8, 8, "EMUUI DS QA", 2, COLOR(26, 20, 31));
    text(top, 8, 30, "TOP DISPLAY", 1, white);
    text(top, 8, 112, "A +1   B +10", 2, white);
    text(top, 8, 136, "DPAD MOVES MARKER", 1, white);
    text(top, 8, 152, "START FREEZE FRAMES", 1, white);
    text(top, 8, 168, "X RESET STATE TO 17", 1, white);
    draw_bottom(bottom, x, y, touches, false);
    for (;;) {
        swiWaitForVBlank();
        scanKeys();
        uint32_t down = keysDown();
        uint32_t held = keysHeld();
        bool isTouch = (held & KEY_TOUCH) != 0;
        bool redrawBottom = isTouch != wasTouch;
        if (down & KEY_A) ++state;
        if (down & KEY_B) state += 10;
        if (down & KEY_X) state = 17;
        if (down & KEY_START) frozen = !frozen;
        if (!frozen) ++frames;
        if (held & KEY_LEFT) { x -= 2; redrawBottom = true; }
        if (held & KEY_RIGHT) { x += 2; redrawBottom = true; }
        if (held & KEY_UP) { y -= 2; redrawBottom = true; }
        if (held & KEY_DOWN) { y += 2; redrawBottom = true; }
        if (isTouch) {
            touchPosition touch;
            touchRead(&touch);
            x = touch.px;
            y = touch.py;
            if (!wasTouch) ++touches;
            redrawBottom = true;
        }
        x = clamp(x, 0, WIDTH - 1);
        y = clamp(y, 0, HEIGHT - 1);
        wasTouch = isTouch;
        rect(top, 8, 48, 248, 55, topColor);
        snprintf(line, sizeof(line), "STATE %06lu", (unsigned long)(state % 1000000));
        text(top, 8, 48, line, 2, COLOR(31, 27, 5));
        snprintf(line, sizeof(line), "FRAME %010lu", (unsigned long)frames);
        text(top, 8, 75, line, 1, white);
        text(top, 8, 92, frozen ? "FROZEN" : "RUNNING", 1, COLOR(12, 31, 24));
        if (redrawBottom) draw_bottom(bottom, x, y, touches, isTouch);
    }
}
