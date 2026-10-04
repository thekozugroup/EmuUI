/* SPDX-License-Identifier: GPL-3.0-or-later */
#ifndef LIBRETRODROID_RENDERREGION_H
#define LIBRETRODROID_RENDERREGION_H

#include <array>
#include <string>
#include <utility>
#include <vector>

#include "utils/rect.h"

namespace libretrodroid {

// Source and destination use normalized top-left coordinates. Regions describe
// the unrotated core image; the default (empty) path retains core rotation.
struct RenderRegion {
    Rect source;
    Rect destination;
};

struct RenderRegionGeometry {
    std::array<float, 12> vertices;
    std::array<float, 12> coordinates;
};

// Core duplicate frames may be skipped, but presentation changes must redraw
// the last texture even when retro_video_refresh receives a null data pointer.
class PresentationState {
public:
    void invalidate() { dirty = true; }
    bool consumeFrame(bool skipDuplicateFrames) {
        if (skipDuplicateFrames && !dirty) return false;
        dirty = false;
        return true;
    }
private:
    bool dirty = false;
};

bool isValidRenderRegion(const RenderRegion& region);
RenderRegionGeometry buildRenderRegionGeometry(const RenderRegion& region, bool bottomLeftOrigin);
std::array<float, 4> renderRegionSampleBounds(
    const RenderRegion& region, bool bottomLeftOrigin, float textureWidth, float textureHeight);
std::string withRenderRegionSampling(const std::string& fragmentSource);
std::pair<float, float> mapRenderRegionPointer(
    const std::vector<RenderRegion>& regions, float windowX, float windowY);
float renderRegionDensity(
    const RenderRegion& region, float screenWidth, float screenHeight,
    float textureWidth, float textureHeight);
float resolveAspectRatio(float reported, unsigned width, unsigned height);
float displayAspectRatio(float rawAspectRatio, float rotationRadians);
float resolveDisplayAspectRatio(float reported, unsigned width, unsigned height, float rotationRadians);

} // namespace libretrodroid
#endif
