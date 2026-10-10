package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink

@Composable
fun GameMenuStatesScreen(
    viewModel: GameMenuStatesViewModel,
    onStateClicked: (Int) -> Unit,
) {
    val state = viewModel.uiStates.collectAsState(initial = GameMenuStatesViewModel.State())

    LemuroidSettingsPage {
        state.value.entries.forEachIndexed { index, entry ->
            LemuroidCardSettingsGroup {
                LemuroidSettingsMenuLink(
                    title = { Text(text = entry.title) },
                    subtitle = { Text(text = entry.description) },
                    enabled = entry.enabled,
                    icon = {
                        if (entry.preview != null) {
                            Image(
                                modifier = Modifier.width(88.dp).height(66.dp).clip(RoundedCornerShape(10.dp)),
                                bitmap = entry.preview.asImageBitmap(),
                                contentScale = ContentScale.Fit,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { onStateClicked(index) },
                )
            }
        }
    }
}
