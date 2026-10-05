package com.swordfish.lemuroid.app.mobile.feature.billing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Client preparation only: no production identity/verifier has been configured. */
class BillingActivity : ComponentActivity() {
    private val products = BillingProducts()
    private val verifier = UnconfiguredPurchaseVerifier
    private val identity: BillingIdentity? = null
    private lateinit var store: PlayBillingStore
    private lateinit var controller: EntitlementController
    private var offer by mutableStateOf<MonthlyOffer?>(null)
    private var message by mutableStateOf<String?>(null)
    private var exporting by mutableStateOf(false)
    private var buying by mutableStateOf(false)
    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) exportSaves(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PlayBillingStore(this, products) { update ->
            lifecycleScope.launch {
                when (update) {
                    PurchaseUpdate.Reconcile -> controller.refresh()
                    PurchaseUpdate.Canceled -> controller.purchaseCanceled()
                    PurchaseUpdate.Unavailable -> controller.purchaseUnavailable()
                }
            }
        }
        controller = EntitlementController(store, verifier, products, SystemClock::elapsedRealtime)
        // No identity is invented. bindOwner must be connected to approved authentication.
        controller.bindOwner(identity?.ownerId)
        setContent {
            val state by controller.state.collectAsState()
            val now by produceState(SystemClock.elapsedRealtime()) {
                while (true) { delay(1000); value = SystemClock.elapsedRealtime() }
            }
            AppTheme(updateSystemBarIcons = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.billing_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.billing_description))
                        Text(stringResource(when (state.status) {
                            BillingStatus.NOT_CONFIGURED -> R.string.billing_not_available
                            BillingStatus.CHECKING -> R.string.billing_checking
                            BillingStatus.PENDING -> R.string.billing_pending
                            BillingStatus.UNAVAILABLE -> R.string.billing_verification_unavailable
                            BillingStatus.CANCELED -> R.string.billing_canceled
                            BillingStatus.READY -> if (state.lease?.access?.lifetime == true && state.allows(AccessAction.START_GAME, now)) R.string.billing_lifetime_owned else
                                if (state.allows(AccessAction.START_GAME, now)) R.string.billing_subscribed else R.string.billing_no_access
                        }))
                        offer?.let { monthly ->
                            Text(stringResource(R.string.billing_monthly_price, monthly.formattedPrice))
                            Text(stringResource(R.string.billing_renewal, monthly.formattedPrice))
                        }
                        Button(onClick = { buyMonthly() }, enabled = offer != null && products.confirmed && verifier.configured && identity != null &&
                            state.status == BillingStatus.READY && !state.pending && !buying &&
                            !state.allows(AccessAction.START_GAME, SystemClock.elapsedRealtime())) {
                            Text(stringResource(R.string.billing_subscribe))
                        }
                        OutlinedButton(onClick = { refresh() }, enabled = state.status != BillingStatus.CHECKING) {
                            Text(stringResource(R.string.billing_restore))
                        }
                        Text(stringResource(R.string.billing_gifts))
                        if (state.shouldManageSubscriptionAfterGift) Text(stringResource(R.string.billing_gift_manage_warning))
                        TextButton(onClick = { openSubscriptionManagement() }) {
                            Text(stringResource(R.string.billing_manage))
                        }
                        HorizontalDivider()
                        Text(stringResource(R.string.billing_saves_retained))
                        OutlinedButton(onClick = {
                            if (GameProcessLock.isHeldByAnotherProcess(applicationContext)) {
                                message = getString(R.string.billing_export_close_game)
                            } else exportDocument.launch("EmuUI-saves.zip")
                        }, enabled = !exporting) { Text(stringResource(R.string.billing_export_saves)) }
                        message?.let { Text(it) }
                        TextButton(onClick = { finish() }) { Text(stringResource(R.string.billing_back)) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::controller.isInitialized) refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            controller.refresh()
            offer = if (verifier.configured) store.monthlyOffer() else null
        }
    }

    private fun buyMonthly() {
        if (!products.confirmed || !verifier.configured || buying) return
        val displayed = offer ?: return
        buying = true
        lifecycleScope.launch {
            try {
                controller.refresh()
                val state = controller.state.value
                if (state.status != BillingStatus.READY || state.pending || state.allows(AccessAction.START_GAME, SystemClock.elapsedRealtime())) return@launch
                val current = store.monthlyOffer()
                offer = current
                if (current == null) {
                    message = getString(R.string.billing_not_available)
                } else if (!displayed.sameTerms(current)) {
                    message = getString(R.string.billing_price_changed)
                } else if (!store.launchMonthly(this@BillingActivity, current, verifier.configured, identity)) {
                    controller.purchaseUnavailable()
                }
            } finally { buying = false }
        }
    }

    private fun openSubscriptionManagement() {
        // General center works before the tentative product IDs/package are published too.
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))) }
            .onFailure { message = getString(R.string.billing_manage_unavailable) }
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

    override fun onDestroy() {
        if (::store.isInitialized) store.close()
        super.onDestroy()
    }
}
