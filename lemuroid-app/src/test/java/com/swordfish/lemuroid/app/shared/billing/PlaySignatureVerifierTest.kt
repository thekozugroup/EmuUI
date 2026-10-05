package com.swordfish.lemuroid.app.shared.billing

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class PlaySignatureVerifierTest {
    // Ephemeral unit-test receipt keys only; unrelated to Android signing/Console keys.
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val verifier = PlaySignatureVerifier(keys.public.encoded, "com.kozudigital.emuui") { Base64.getDecoder().decode(it) }
    private fun receipt(json: String, token: String = "test-token", product: String = "emuui_lifetime", signed: String = json): StorePurchase {
        val signer = Signature.getInstance("SHA1withRSA").apply { initSign(keys.private); update(signed.toByteArray()) }
        return StorePurchase(product, token, PurchaseState.PURCHASED, originalJson = json,
            signature = Base64.getEncoder().encodeToString(signer.sign()))
    }
    private val json = """{"packageName":"com.kozudigital.emuui","productId":"emuui_lifetime","purchaseToken":"test-token","purchaseState":0}"""
    @Test fun signedReceiptWithoutOrderIdWorks() { assertTrue(verifier.verifies(receipt(json))) }
    @Test fun tamperedReceiptFails() { assertFalse(verifier.verifies(receipt(json.replace("test-token", "changed"), signed = json))) }
    @Test fun wrongPackageFails() { assertFalse(verifier.verifies(receipt(json.replace("com.kozudigital.emuui", "another.app")))) }
    @Test fun tokenAndProductMustMatchSignedPayload() {
        assertFalse(verifier.verifies(receipt(json, token = "another")))
        assertFalse(verifier.verifies(receipt(json, product = "emuui_monthly")))
    }
    @Test fun signedPendingAndMalformedPayloadFail() {
        assertFalse(verifier.verifies(receipt(json.replace(":0", ":4"))))
        assertFalse(verifier.verifies(receipt("not JSON")))
    }
    @Test fun incorrectKeyFails() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertFalse(PlaySignatureVerifier(other.public.encoded, "com.kozudigital.emuui") { Base64.getDecoder().decode(it) }.verifies(receipt(json)))
    }
    @Test fun missingKeyCannotConfigure() { assertFalse(PlaySignatureVerifier(byteArrayOf(), "com.kozudigital.emuui") { byteArrayOf() }.configured) }
}
