package com.swordfish.lemuroid.app.mobile.feature.home

import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager

private enum class PreparedMenuFocus { INITIAL, EXISTING, UNAVAILABLE }

/** Pointer presses can drop system-defined Button focus before the virtual key's click arrives. */
@OptIn(ExperimentalComposeUiApi::class)
private fun prepareMenuFocus(
    inputModeManager: InputModeManager,
    requester: FocusRequester,
    hasFocus: () -> Boolean,
): PreparedMenuFocus {
    val alreadyFocused = hasFocus()
    if (!inputModeManager.requestInputMode(InputMode.Keyboard)) return PreparedMenuFocus.UNAVAILABLE
    if (alreadyFocused || requester.restoreFocusedChild()) return PreparedMenuFocus.EXISTING
    val acquired = requester.requestFocus(FocusDirection.Enter)
    return if (acquired) PreparedMenuFocus.INITIAL else PreparedMenuFocus.UNAVAILABLE
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun moveLauncherMenuFocus(
    inputModeManager: InputModeManager,
    focusManager: FocusManager,
    requester: FocusRequester,
    hasFocus: () -> Boolean,
    direction: FocusDirection,
) {
    when (prepareMenuFocus(inputModeManager, requester, hasFocus)) {
        PreparedMenuFocus.EXISTING -> focusManager.moveFocus(direction)
        PreparedMenuFocus.INITIAL -> Unit
        PreparedMenuFocus.UNAVAILABLE -> return
    }
    // Keep the last valid child across the next real touch. Detached route children cannot restore.
    requester.saveFocusedChild()
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun activateLauncherMenuFocus(
    inputModeManager: InputModeManager,
    requester: FocusRequester,
    hasFocus: () -> Boolean,
    view: View,
) {
    if (prepareMenuFocus(inputModeManager, requester, hasFocus) == PreparedMenuFocus.UNAVAILABLE) return
    // Activation can synchronously close this dialog or navigate away: save before dispatching.
    requester.saveFocusedChild()
    view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
    view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
}
