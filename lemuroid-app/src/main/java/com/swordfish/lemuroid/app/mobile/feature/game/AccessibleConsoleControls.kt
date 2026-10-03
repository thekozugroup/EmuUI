package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.touchinput.radial.settings.TouchControllerID

internal fun supportsAccessibleConsole(controller: TouchControllerID): Boolean =
    controller in
        setOf(
            TouchControllerID.GB, TouchControllerID.GBA, TouchControllerID.NES,
            TouchControllerID.SNES, TouchControllerID.MELONDS, TouchControllerID.DESMUME,
        )

/** Native button semantics for TalkBack/Switch Access, with explicit held-button actions. */
@Composable
internal fun AccessibleConsoleControls(
    left: Boolean,
    controller: TouchControllerID,
    viewModel: BaseGameScreenViewModel,
) {
    val held = viewModel.getAccessibleHeldButtons().collectAsState(emptySet()).value
    DisposableEffect(viewModel) {
        onDispose { viewModel.releaseVirtualControls() }
    }
    val buttons =
        if (left) {
            listOf(
                "Up" to KeyEvent.KEYCODE_DPAD_UP,
                "Down" to KeyEvent.KEYCODE_DPAD_DOWN,
                "Left" to KeyEvent.KEYCODE_DPAD_LEFT,
                "Right" to KeyEvent.KEYCODE_DPAD_RIGHT,
                "Select" to KeyEvent.KEYCODE_BUTTON_SELECT,
            ) +
                when (controller) {
                    TouchControllerID.MELONDS ->
                        listOf(
                            "Microphone" to KeyEvent.KEYCODE_BUTTON_L2,
                            "Close lid" to KeyEvent.KEYCODE_BUTTON_THUMBL,
                        )
                    TouchControllerID.DESMUME ->
                        listOf(
                            "Microphone" to KeyEvent.KEYCODE_BUTTON_THUMBL,
                            "Close lid" to KeyEvent.KEYCODE_BUTTON_L2,
                        )
                    else -> emptyList()
                }
        } else {
            buildList {
                add("A" to KeyEvent.KEYCODE_BUTTON_A)
                add("B" to KeyEvent.KEYCODE_BUTTON_B)
                if (controller in setOf(TouchControllerID.SNES, TouchControllerID.MELONDS, TouchControllerID.DESMUME)) {
                    add("X" to KeyEvent.KEYCODE_BUTTON_X)
                    add("Y" to KeyEvent.KEYCODE_BUTTON_Y)
                }
                if (controller !in setOf(TouchControllerID.GB, TouchControllerID.NES)) {
                    add("L" to KeyEvent.KEYCODE_BUTTON_L1)
                    add("R" to KeyEvent.KEYCODE_BUTTON_R1)
                }
                add("Start" to KeyEvent.KEYCODE_BUTTON_START)
            }
        }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        buttons.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.Center) {
                row.forEach { (label, code) ->
                    TextButton(
                        onClick = {
                            if (code in held) {
                                viewModel.setAccessibleButton(code, false)
                            } else {
                                viewModel.tapAccessibleButton(code)
                            }
                        },
                        modifier =
                            Modifier.sizeIn(minWidth = 64.dp, minHeight = 48.dp).semantics {
                                contentDescription = "$label button"
                                stateDescription = if (code in held) "Held" else "Released"
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction("Hold $label") {
                                            viewModel.setAccessibleButton(code, true)
                                            true
                                        },
                                        CustomAccessibilityAction("Release $label") {
                                            viewModel.setAccessibleButton(code, false)
                                            true
                                        },
                                    )
                            },
                    ) { Text(if (code in held) "$label •" else label, color = Color.White) }
                }
            }
        }
    }
}
