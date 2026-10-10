package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LemuroidGameCard(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { },
) {
    val haptics = LocalConsoleHaptics.current
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { haptics.press(); onClick() },
                        onLongClick = { haptics.press(); onLongClick() },
                    ),
        ) {
            LemuroidGameImage(game = game)
            LemuroidGameTexts(game = game)
        }
    }
}
