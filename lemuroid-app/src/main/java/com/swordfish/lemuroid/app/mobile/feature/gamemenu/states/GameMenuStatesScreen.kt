package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage

@Composable
fun GameMenuStatesScreen(
    viewModel: GameMenuStatesViewModel,
    onStateClicked: (Int) -> Unit,
) {
    val state = viewModel.uiStates.collectAsState().value
    SaveSlotsContent(state, viewModel::retry, onStateClicked)
}

@Composable
internal fun SaveSlotsContent(
    state: GameMenuStatesViewModel.State,
    onRetry: () -> Unit,
    onStateClicked: (Int) -> Unit,
) {
    LemuroidSettingsPage {
        if (state.isLoading) {
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Reading save slots…", style = MaterialTheme.typography.bodyMedium)
            }
        } else if (state.hasError) {
            LemuroidCardSettingsGroup {
                Column(
                    Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Save slots could not be opened", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your saves have not been changed. Try reading them again.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
        state.entries.forEachIndexed { index, entry ->
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
