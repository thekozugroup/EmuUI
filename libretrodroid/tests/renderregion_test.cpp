/* SPDX-License-Identifier: GPL-3.0-or-later */
#include <cassert>
#include <cmath>
#include <iostream>
#include <limits>
#include <string>

#include "renderregion.h"
#include "shadermanager.h"
#include "videolayout.h"

using namespace libretrodroid;
namespace {
int checks = 0;
void near(float actual, float expected) {
    ++checks;
    assert(std::isfinite(actual));
    assert(std::abs(actual - expected) < 0.00001f);
}
void check(bool result) { ++checks; assert(result); }
void point(std::pair<float, float> actual, float x, float y) {
    near(actual.first, x);
    near(actual.second, y);
}
}

int main() {
    PresentationState presentation;
    check(!presentation.consumeFrame(true));
    for (int geometryChange = 0; geometryChange < 7; ++geometryChange) {
        presentation.invalidate(); // viewport, surface, ratio, rotation, regions, shader, renderer
        check(presentation.consumeFrame(true)); // redraw the duplicate source frame
        check(!presentation.consumeFrame(true));
    }
    check(presentation.consumeFrame(false));
    check(presentation.consumeFrame(false));

    const RenderRegion top {Rect(0, 0, 1, .5f), Rect(.1f, .02f, .8f, .36f)};
    const RenderRegion lower {Rect(0, .5f, 1, .5f), Rect(.3f, .6f, .4f, .3f)};
    const std::vector<RenderRegion> regions {top, lower};
    check(isValidRenderRegion(top));
    check(isValidRenderRegion(lower));
    for (float bad : {-1.0f, std::numeric_limits<float>::infinity(), std::numeric_limits<float>::quiet_NaN()}) {
        check(!isValidRenderRegion({Rect(bad, 0, 1, .5f), lower.destination}));
        check(!isValidRenderRegion({lower.source, Rect(0, bad, .5f, .5f)}));
    }
    check(!isValidRenderRegion({Rect(0, 0, 0, 1), lower.destination}));
    check(!isValidRenderRegion({lower.source, Rect(0, 0, 1, 0)}));
    check(!isValidRenderRegion({lower.source, Rect(.6f, 0, .5f, 1)}));

    auto geometry = buildRenderRegionGeometry(lower, false);
    near(geometry.vertices[0], -.4f);
    near(geometry.vertices[1], -.2f);
    near(geometry.vertices[10], .4f);
    near(geometry.vertices[11], -.8f);
    near(geometry.coordinates[0], 0);
    near(geometry.coordinates[1], .5f);
    near(geometry.coordinates[10], 1);
    near(geometry.coordinates[11], 1);
    auto hardware = buildRenderRegionGeometry(lower, true);
    check(hardware.vertices == geometry.vertices);
    near(hardware.coordinates[1], .5f);
    near(hardware.coordinates[11], 0);

    // Window coordinates remain [-1, 1], source pointer coordinates [0, 1].
    point(mapRenderRegionPointer(regions, -.4f, .2f), 0, .5f);
    point(mapRenderRegionPointer(regions, 0, .5f), .5f, .75f);
    point(mapRenderRegionPointer(regions, .4f, .8f), 1, 1);
    point(mapRenderRegionPointer(regions, 0, 0), -10, -10); // Hinge/gap
    point(mapRenderRegionPointer(regions, -.9f, .5f), -10, -10); // Controls
    point(mapRenderRegionPointer(regions, -10, 10), -10, -10); // ACTION_UP/CANCEL
    point(mapRenderRegionPointer(regions, 0, std::numeric_limits<float>::quiet_NaN()), -10, -10);
    point(mapRenderRegionPointer({}, 0, 0), -10, -10);
    point(mapRenderRegionPointer({top, {lower.source, top.destination}}, 0, -.6f), .5f, .75f);

    // Every edge and interior point of the touch image round-trips for both sizes.
    for (int ix = 0; ix <= 8; ++ix) {
        for (int iy = 0; iy <= 8; ++iy) {
            const float u = ix / 8.0f;
            const float v = iy / 8.0f;
            const float x = 2 * (lower.destination.getX() + lower.destination.getWidth() * u) - 1;
            const float y = 2 * (lower.destination.getY() + lower.destination.getHeight() * v) - 1;
            point(mapRenderRegionPointer(regions, x, y), u, .5f + v * .5f);
        }
    }

    // Clamp samples to source texel centers without shrinking the displayed quad.
    for (bool bottomLeft : {false, true}) {
        for (float scale : {1.0f, 2.0f, 4.0f}) {
            const auto bounds = renderRegionSampleBounds(lower, bottomLeft, 256 * scale, 384 * scale);
            near(bounds[0], .5f / (256 * scale));
            near(bounds[2], 1 - .5f / (256 * scale));
            near(bounds[1], (bottomLeft ? 0 : .5f) + .5f / (384 * scale));
            near(bounds[3], (bottomLeft ? .5f : 1) - .5f / (384 * scale));
            check(bounds[0] <= bounds[2] && bounds[1] <= bounds[3]);
        }
    }
    const auto tinyBounds = renderRegionSampleBounds({Rect(0, 0, .001f, .001f), lower.destination}, false, 256, 384);
    near(tinyBounds[0], tinyBounds[2]);
    near(tinyBounds[1], tinyBounds[3]);
    near(renderRegionDensity(lower, 1000, 1000, 256, 384), 1.5625f);
    near(renderRegionDensity(lower, 1000, 1000, 0, 0), 1);
    near(resolveAspectRatio(4.0f / 3, 256, 224), 4.0f / 3);
    for (float ratio : {0.0f, -1.0f, std::numeric_limits<float>::infinity(), std::numeric_limits<float>::quiet_NaN()}) {
        near(resolveAspectRatio(ratio, 256, 192), 4.0f / 3);
    }
    near(resolveAspectRatio(0, 0, 0), 0);

    // Explicit DAR is already display-oriented; only the pixel-ratio fallback
    // is rotated. The same effective value is fitted by native VideoLayout.
    constexpr float quarterTurn = 1.5707963267948966f;
    for (int turn = -4; turn <= 4; ++turn) {
        const float rotation = turn * quarterTurn;
        const float expectedPixels = std::abs(turn) % 2 == 1 ? .75f : 4.0f / 3;
        near(resolveDisplayAspectRatio(4.0f / 3, 256, 192, rotation), 4.0f / 3);
        near(resolveDisplayAspectRatio(.75f, 256, 192, rotation), .75f);
        near(resolveDisplayAspectRatio(0, 256, 192, rotation), expectedPixels);
        near(resolveDisplayAspectRatio(-1, 256, 192, rotation), expectedPixels);
        near(resolveDisplayAspectRatio(std::numeric_limits<float>::quiet_NaN(), 256, 192, rotation), expectedPixels);
        for (float effective : {expectedPixels, 4.0f / 3, .75f}) {
            VideoLayout rotated(false, rotation, Rect(.1f, .05f, .8f, .45f));
            rotated.updateScreenSize(1000, 800);
            rotated.updateAspectRatio(effective);
            const auto& vertices = rotated.getForegroundVertices();
            float minX = 1, maxX = -1, minY = 1, maxY = -1;
            for (size_t i = 0; i < vertices.size(); i += 2) {
                minX = std::min(minX, vertices[i]);
                maxX = std::max(maxX, vertices[i]);
                minY = std::min(minY, vertices[i + 1]);
                maxY = std::max(maxY, vertices[i + 1]);
            }
            const float width = (maxX - minX) * 500;
            const float height = (maxY - minY) * 400;
            near(width / height, effective);
            check(width <= 800.001f && height <= 360.001f);
            check(std::abs(width - 800) < .001f || std::abs(height - 360) < .001f);
        }
    }
    near(displayAspectRatio(0, quarterTurn), 0);
    near(displayAspectRatio(4.0f / 3, std::numeric_limits<float>::quiet_NaN()), 4.0f / 3);

    // Empty-region restore leaves the original native-aspect fit and pointer transform intact.
    VideoLayout layout(false, 0, Rect(0, 0, 1, .5f));
    layout.updateScreenSize(1000, 800);
    layout.updateAspectRatio(4.0f / 3);
    const auto originalVertices = layout.getForegroundVertices();
    near(originalVertices[0], -.533333333f);
    near(originalVertices[1], 1);
    point(layout.getRelativePosition(0, -.5f), .5f, .5f);
    layout.updateRenderRegions(regions);
    point(layout.getRelativePosition(0, .5f), .5f, .75f);
    layout.updateRenderRegions({});
    check(layout.getForegroundVertices() == originalVertices);
    point(layout.getRelativePosition(0, -.5f), .5f, .5f);
    point(layout.getRelativePosition(0, .5f), -10, -10);

    for (int type = 0; type <= 6; ++type) {
        const auto chain = ShaderManager::getShader({static_cast<ShaderManager::Type>(type), {}});
        check(!chain.passes.empty());
        const auto wrapped = withRenderRegionSampling(chain.passes.back().fragment);
        check(wrapped.find("uniform bool emuuiClampSource") != std::string::npos);
        check(wrapped.find("texture2D(texture,") == std::string::npos);
        check(wrapped.find("texture2D(previousPass,") == std::string::npos);
        check(wrapped.find("texture2D(image,") != std::string::npos);
    }
    std::cout << "PASS: " << checks << " render-region, pointer, aspect and shader-source checks\n";
}
