package com.swordfish.lemuroid.app.shared.game

/** Restore background scheduling whenever preparation started but no game Activity was launched. */
internal suspend fun launchWithBackgroundRecovery(
    prepare: suspend () -> Unit,
    launch: suspend () -> Boolean,
    recover: () -> Unit,
) {
    var launched = false
    try {
        prepare()
        launched = launch()
    } finally {
        if (!launched) recover()
    }
}
