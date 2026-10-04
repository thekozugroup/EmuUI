/* SPDX-License-Identifier: GPL-3.0-or-later */
#include "renderregion.h"

#include <algorithm>
#include <cmath>

namespace libretrodroid {
namespace {
bool isValidRect(const Rect& rect) {
    return std::isfinite(rect.getX()) && std::isfinite(rect.getY()) &&
           std::isfinite(rect.getWidth()) && std::isfinite(rect.getHeight()) &&
           rect.getX() >= 0.0f && rect.getY() >= 0.0f &&
           rect.getWidth() > 0.0f && rect.getHeight() > 0.0f &&
           rect.getX() + rect.getWidth() <= 1.000001f &&
           rect.getY() + rect.getHeight() <= 1.000001f;
}
}

bool isValidRenderRegion(const RenderRegion& region) {
    return isValidRect(region.source) && isValidRect(region.destination);
}

RenderRegionGeometry buildRenderRegionGeometry(const RenderRegion& region, bool bottomLeftOrigin) {
    const auto& dst = region.destination;
    const auto& src = region.source;
    const float left = 2.0f * dst.getX() - 1.0f;
    const float right = 2.0f * (dst.getX() + dst.getWidth()) - 1.0f;
    const float top = 1.0f - 2.0f * dst.getY();
    const float bottom = 1.0f - 2.0f * (dst.getY() + dst.getHeight());
    const float u0 = src.getX();
    const float u1 = src.getX() + src.getWidth();
    const float v0 = bottomLeftOrigin ? 1.0f - src.getY() : src.getY();
    const float v1 = bottomLeftOrigin ? 1.0f - src.getY() - src.getHeight() : src.getY() + src.getHeight();
    return {
        {left, top, left, bottom, right, top, right, top, left, bottom, right, bottom},
        {u0, v0, u0, v1, u1, v0, u1, v0, u0, v1, u1, v1},
    };
}

std::array<float, 4> renderRegionSampleBounds(
    const RenderRegion& region, bool bottomLeftOrigin, float textureWidth, float textureHeight) {
    const auto& src = region.source;
    const float halfX = textureWidth > 0.0f ? std::min(0.5f / textureWidth, src.getWidth() * 0.5f) : 0.0f;
    const float halfY = textureHeight > 0.0f ? std::min(0.5f / textureHeight, src.getHeight() * 0.5f) : 0.0f;
    const float y = bottomLeftOrigin ? 1.0f - src.getY() - src.getHeight() : src.getY();
    return {src.getX() + halfX, y + halfY,
            src.getX() + src.getWidth() - halfX, y + src.getHeight() - halfY};
}

std::string withRenderRegionSampling(const std::string& fragmentSource) {
    // All built-in fragment shaders use texture2D. Clamp the sample itself, not
    // interpolated UVs, so the image retains its exact pixel mapping and extent.
    std::string bounded = fragmentSource;
    const std::string original = "texture2D(";
    const std::string replacement = "emuuiTexture2D(";
    size_t offset = 0;
    while ((offset = bounded.find(original, offset)) != std::string::npos) {
        bounded.replace(offset, original.size(), replacement);
        offset += replacement.size();
    }
    return
        "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
        "#define EMUUI_PRECISION highp\n"
        "#else\n"
        "#define EMUUI_PRECISION mediump\n"
        "#endif\n"
        "uniform EMUUI_PRECISION vec4 emuuiSourceBounds;\n"
        "uniform bool emuuiClampSource;\n"
        "lowp vec4 emuuiTexture2D(lowp sampler2D image, EMUUI_PRECISION vec2 uv) {\n"
        "  return texture2D(image, emuuiClampSource ? clamp(uv, emuuiSourceBounds.xy, emuuiSourceBounds.zw) : uv);\n"
        "}\n" + bounded;
}

std::pair<float, float> mapRenderRegionPointer(
    const std::vector<RenderRegion>& regions, float windowX, float windowY) {
    if (!std::isfinite(windowX) || !std::isfinite(windowY) ||
        windowX < -1.0f || windowX > 1.0f || windowY < -1.0f || windowY > 1.0f) {
        return {-10.0f, -10.0f};
    }
    const float x = (windowX + 1.0f) * 0.5f;
    const float y = (windowY + 1.0f) * 0.5f;
    // Last rendered region wins if a caller intentionally overlaps destinations.
    for (auto it = regions.rbegin(); it != regions.rend(); ++it) {
        const auto& dst = it->destination;
        if (x >= dst.getX() && x <= dst.getX() + dst.getWidth() &&
            y >= dst.getY() && y <= dst.getY() + dst.getHeight()) {
            return {
                it->source.getX() + (x - dst.getX()) / dst.getWidth() * it->source.getWidth(),
                it->source.getY() + (y - dst.getY()) / dst.getHeight() * it->source.getHeight(),
            };
        }
    }
    return {-10.0f, -10.0f};
}

float renderRegionDensity(
    const RenderRegion& region, float screenWidth, float screenHeight,
    float textureWidth, float textureHeight) {
    if (textureWidth <= 0.0f || textureHeight <= 0.0f) return 1.0f;
    return std::min(
        region.destination.getWidth() * screenWidth / (region.source.getWidth() * textureWidth),
        region.destination.getHeight() * screenHeight / (region.source.getHeight() * textureHeight));
}

float resolveAspectRatio(float reported, unsigned width, unsigned height) {
    if (std::isfinite(reported) && reported > 0.0f) return reported;
    return width > 0 && height > 0 ? static_cast<float>(width) / static_cast<float>(height) : 0.0f;
}

float displayAspectRatio(float rawAspectRatio, float rotationRadians) {
    if (!std::isfinite(rawAspectRatio) || rawAspectRatio <= 0.0f) return 0.0f;
    if (!std::isfinite(rotationRadians)) return rawAspectRatio;
    // RETRO_ENVIRONMENT_SET_ROTATION specifies integer quarter turns. Reduce
    // before rounding so both signs and a wrapped full turn are well-defined.
    constexpr float quarterTurn = 1.5707963267948966f;
    const int turns = static_cast<int>(std::lround(std::remainder(rotationRadians, 4 * quarterTurn) / quarterTurn));
    return std::abs(turns) % 2 == 1 ? 1.0f / rawAspectRatio : rawAspectRatio;
}

float resolveDisplayAspectRatio(float reported, unsigned width, unsigned height, float rotationRadians) {
    // A positive libretro geometry.aspect_ratio is already the requested DAR;
    // cores such as melonDS DS explicitly account for their own rotation in it.
    // Rotate only our square-pixel width/height fallback, never explicit DAR.
    if (std::isfinite(reported) && reported > 0.0f) return reported;
    return displayAspectRatio(resolveAspectRatio(reported, width, height), rotationRadians);
}
} // namespace libretrodroid
