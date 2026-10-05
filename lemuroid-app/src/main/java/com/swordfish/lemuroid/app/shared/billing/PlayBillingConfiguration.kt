package com.swordfish.lemuroid.app.shared.billing

import android.util.Base64
import com.swordfish.lemuroid.BuildConfig

/** Public Console configuration only. No service account, EmuUI login or backend. */
object PlayBillingConfiguration {
    val products = BillingProducts()
    // Copy this app's public RSA licensing key after Console setup; never a keystore/private key.
    private const val PUBLIC_LICENSE_KEY = ""
    val enforceGameAccess: Boolean get() = !BuildConfig.DEBUG && BuildConfig.FLAVOR.startsWith("play")
    fun verifier(): PurchaseVerifier {
        val key = runCatching { Base64.decode(PUBLIC_LICENSE_KEY, Base64.DEFAULT) }.getOrNull()
        return if (key == null || key.isEmpty()) UnconfiguredPurchaseVerifier else
            PlaySignatureVerifier(key, BuildConfig.APPLICATION_ID) { Base64.decode(it, Base64.DEFAULT) }
    }
}
