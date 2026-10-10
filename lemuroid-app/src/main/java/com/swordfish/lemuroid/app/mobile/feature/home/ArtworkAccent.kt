package com.swordfish.lemuroid.app.mobile.feature.home

/** Dominant chromatic bucket from a small local sample; blank/monochrome art stays neutral. */
internal fun dominantArtworkColor(pixels: IntArray): Int? {
    val counts = IntArray(512)
    val red = IntArray(512)
    val green = IntArray(512)
    val blue = IntArray(512)
    for (pixel in pixels) {
        val a = pixel ushr 24
        val r = pixel ushr 16 and 255
        val g = pixel ushr 8 and 255
        val b = pixel and 255
        val high = maxOf(r, g, b)
        val low = minOf(r, g, b)
        if (a < 128 || high < 40 || low > 225 || high - low < 24) continue
        val bucket = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
        counts[bucket]++
        red[bucket] += r
        green[bucket] += g
        blue[bucket] += b
    }
    val bucket = counts.indices.maxByOrNull { counts[it] } ?: return null
    val count = counts[bucket]
    if (count == 0) return null
    return (255 shl 24) or (red[bucket] / count shl 16) or
        (green[bucket] / count shl 8) or (blue[bucket] / count)
}
