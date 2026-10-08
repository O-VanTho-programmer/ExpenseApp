package com.antigravity.expensetracker.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the generation and hardware-backed storage of the SQLCipher database encryption key
 * using the Android KeyStore and AES-256-GCM (OWASP MASVS-CRYPTO / MASVS-STORAGE).
 */
class DatabaseKeyManager(private val context: Context) {

    private val keyStoreAlias = "ExpenseTrackerDatabaseKeyAlias"
    private val prefName = "expense_security_prefs"
    private val prefEncryptedKey = "enc_db_passphrase"
    private val prefIv = "enc_db_iv"

    fun getDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(prefEncryptedKey, null)
        val ivBase64 = prefs.getString(prefIv, null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            try {
                val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                return decryptKey(encryptedBytes, iv)
            } catch (e: Exception) {
                // Fail-closed: Never overwrite an existing key on decryption failure,
                // as doing so would render existing encrypted financial data permanently unrecoverable.
                throw SecurityException("Failed to decrypt database encryption key with AndroidKeyStore: ${e.message}", e)
            }
        }

        // Generate a new cryptographically secure 256-bit (32 bytes) passphrase
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)

        try {
            val (encryptedBytes, iv) = encryptKey(rawPassphrase)
            val success = prefs.edit()
                .putString(prefEncryptedKey, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(prefIv, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit() // Synchronous commit to ensure atomic persistence before DB creation

            if (!success) {
                throw SecurityException("Failed to persist database encryption key to storage")
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt and store database passphrase: ${e.message}", e)
        }

        return rawPassphrase
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(keyStoreAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyStoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return (keyStore.getEntry(keyStoreAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encryptKey(data: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, cipher.iv)
    }

    private fun decryptKey(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encrypted)
    }
}
