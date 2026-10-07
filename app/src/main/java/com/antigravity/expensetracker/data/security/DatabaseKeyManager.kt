package com.antigravity.expensetracker.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
                val encryptedBytes = android.util.Base64.decode(encryptedKeyBase64, android.util.Base64.DEFAULT)
                val iv = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
                return decryptKey(encryptedBytes, iv)
            } catch (e: Exception) {
                // If decryption fails (e.g., keystore invalidated), fallback to generating new or default key
            }
        }

        // Generate a new 256-bit passphrase
        val rawPassphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(rawPassphrase)

        try {
            val (encryptedBytes, iv) = encryptKey(rawPassphrase)
            prefs.edit()
                .putString(prefEncryptedKey, android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT))
                .putString(prefIv, android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            // In unit test or environment without AndroidKeyStore, return rawPassphrase
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
