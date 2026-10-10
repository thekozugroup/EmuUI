package com.swordfish.lemuroid.app.mobile.feature.billing

import android.net.Uri
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalConsoleHaptics
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.shared.billing.*
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Independent save export. All supported games are free, without an account. */
class BillingActivity : ComponentActivity() {
    private var message by mutableStateOf<String?>(null)
    private var exporting by mutableStateOf(false)
    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) exportSaves(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(updateSystemBarIcons = true) {
                val haptics = LocalConsoleHaptics.current
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.billing_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.billing_description))
                        Text(stringResource(R.string.billing_saves_retained))
                        OutlinedButton(onClick = {
                            haptics.press()
                            if (GameProcessLock.isHeldByAnotherProcess(applicationContext)) {
                                message = getString(R.string.billing_export_close_game)
                            } else exportDocument.launch("EmuUI-saves.zip")
                        }, enabled = !exporting) { Text(stringResource(R.string.billing_export_saves)) }
                        message?.let { Text(it) }
                        TextButton(onClick = { haptics.press(); finish() }) { Text(stringResource(R.string.billing_back)) }
                    }
                }
            }
        }
    }

    private fun exportSaves(uri: Uri) {
        if (GameProcessLock.isHeldByAnotherProcess(applicationContext)) {
            message = getString(R.string.billing_export_close_game)
            return
        }
        exporting = true
        lifecycleScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val roots = mutableMapOf("legacy-states" to File(filesDir, "states"))
                    getExternalFilesDir(null)?.let { external ->
                        listOf("saves", "states", "state-previews").forEach { roots[it] = File(external, it) }
                    }
                    // Finish a private snapshot before opening the chosen destination, so
                    // an export inside a save folder cannot recursively include itself.
                    val staged = File.createTempFile("save-export-", ".zip", cacheDir)
                    try {
                        val count = staged.outputStream().use { SaveArchive.write(roots, it) }
                        val output = contentResolver.openOutputStream(uri, "w") ?: error("Export destination unavailable")
                        output.use { destination -> staged.inputStream().use { it.copyTo(destination) } }
                        count
                    } finally { staged.delete() }
                }
                message = getString(R.string.billing_export_done, count)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                message = getString(R.string.billing_export_failed)
            } finally { exporting = false }
        }
    }

}
