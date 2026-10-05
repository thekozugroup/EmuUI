package com.swordfish.lemuroid.app.shared.billing

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import kotlinx.serialization.json.*

/** Local Play signature verification. This proves the receipt was signed, not that
 * Google has not since refunded it. Only current queryPurchases results are accepted.
 * The Console licensing key is PUBLIC configuration, never a private signing key.
 */
class PlaySignatureVerifier(
    publicKeyBytes: ByteArray,
    private val packageName: String,
    private val decodeSignature: (String) -> ByteArray,
) : PurchaseVerifier {
    private val key = runCatching { KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKeyBytes)) }.getOrNull()
    override val configured get() = key != null && packageName.isNotBlank()
    override fun verifies(purchase: StorePurchase): Boolean = runCatching {
        if (!configured || purchase.state != PurchaseState.PURCHASED || purchase.token.isBlank()) return false
        val verifier = Signature.getInstance("SHA1withRSA")
        verifier.initVerify(key)
        verifier.update(purchase.originalJson.toByteArray(Charsets.UTF_8))
        if (!verifier.verify(decodeSignature(purchase.signature))) return false
        val json = Json.parseToJsonElement(purchase.originalJson).jsonObject
        val signedProducts = json["productIds"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: listOfNotNull(json["productId"]?.jsonPrimitive?.content)
        val signedToken = (json["token"] ?: json["purchaseToken"])?.jsonPrimitive?.content
        // Play's signed JSON uses 0 for purchased (different from the public API enum).
        json["packageName"]?.jsonPrimitive?.content == packageName &&
            purchase.productId in signedProducts && signedToken == purchase.token &&
            json["purchaseState"]?.jsonPrimitive?.intOrNull == 0
    }.getOrDefault(false)
}
