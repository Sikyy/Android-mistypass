package com.mistyislet.app.core.ble

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages EC P-256 keypairs in Android Keystore for BLE credential authentication.
 *
 * The private key NEVER leaves the hardware security module (TEE or StrongBox).
 * Only the public key is exported and registered with the MistyPass cloud.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "mistyislet_ble_credential"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /** Checks if a credential keypair already exists. */
    fun hasKeyPair(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    /**
     * Generates a new EC P-256 keypair in Android Keystore.
     * - Private key is hardware-bound and non-exportable.
     * - Prefers StrongBox, falls back to TEE.
     * - No user authentication required (factory workers shouldn't need biometrics to tap).
     */
    fun generateKeyPair(): ByteArray {
        // Delete old key if exists (re-registration)
        if (hasKeyPair()) deleteKeyPair()

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .setInvalidatedByBiometricEnrollment(false)

        // Prefer StrongBox (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (_: Exception) {
                // StrongBox not available, fall back to TEE silently
            }
        }

        // Request attestation challenge (API 24+)
        // TODO: Replace static challenge with a server-provided nonce when server-side attestation verification is implemented
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setAttestationChallenge("mistyislet-attest".toByteArray())
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
        kpg.initialize(builder.build())
        val keyPair = kpg.generateKeyPair()
        return keyPair.public.encoded
    }

    /**
     * V2 signing: SHA256(nonce || userId || transportTag)
     * Transport tag binds signature to a specific channel ("BLE" or "NFC_HCE").
     */
    fun signChallengeV2(nonce: ByteArray, userId: String, transportTag: String): ByteArray {
        require(nonce.size == 32) { "Nonce must be 32 bytes" }
        require(transportTag == "BLE" || transportTag == "NFC_HCE") { "Invalid transport tag" }

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val tagBytes = transportTag.toByteArray(Charsets.UTF_8)
        val message = nonce + userIdBytes + tagBytes

        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
            ?: throw IllegalStateException("BLE credential key not found")

        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(privateKey)
            update(message)
            sign()
        }
    }

    /** Returns the public key in PEM format for cloud registration. */
    fun getPublicKeyPEM(): String {
        val cert = keyStore.getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("No key pair found. Call generateKeyPair() first.")
        val der = cert.publicKey.encoded
        val b64 = Base64.encodeToString(der, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$b64\n-----END PUBLIC KEY-----\n"
    }

    /** Returns the attestation cert chain as Base64-encoded DER certificates. */
    fun getAttestationChain(): List<String> {
        val chain = keyStore.getCertificateChain(KEY_ALIAS) ?: return emptyList()
        return chain.map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
    }

    /** Detects keystore security level: "strongbox", "tee", or "software". */
    fun getKeystoreLevel(): String {
        // Heuristic: if we successfully set StrongBox flag during generation
        // on a device that supports it, we're in StrongBox.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Check if attestation chain mentions strongbox
            val chain = keyStore.getCertificateChain(KEY_ALIAS)
            if (chain != null && chain.size >= 2) {
                val issuer = chain[1].toString().lowercase()
                if (issuer.contains("strongbox")) return "strongbox"
            }
            return "tee"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "tee" else "software"
    }

    /** Deletes the keypair (on revocation or re-registration). */
    fun deleteKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
