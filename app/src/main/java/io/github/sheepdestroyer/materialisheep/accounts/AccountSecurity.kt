package io.github.sheepdestroyer.materialisheep.accounts

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles hardware-backed Keystore AES-GCM encryption for user credentials.
 * Ensures zero-trust security where passwords/tokens are never stored in plaintext AccountManager.
 */
object AccountSecurity {
    private const val TAG = "AccountSecurity"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "materialisheep_master_auth_key"
    private const val PREFS_NAME = "auth_secure_store"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

    private var fallbackKey: SecretKey? = null

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Throwable) {
            // Fallback for JVM test environments (Robolectric) where AndroidKeyStore is unavailable
            if (fallbackKey != null) {
                return fallbackKey!!
            }
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val key = keyGen.generateKey()
            fallbackKey = key
            return key
        }
    }

    /**
     * Encrypts and securely persists the password for the given username.
     */
    @JvmStatic
    fun savePassword(context: Context, username: String, password: String?) {
        if (password.isNullOrEmpty()) {
            clearPassword(context, username)
            return
        }

        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            // Pack IV (12 bytes) + CipherText (with GCM tag)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("pwd_$username", encoded).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save password securely", e)
        }
    }

    /**
     * Decrypts and returns the password for the given username, or null if not found/corrupted.
     */
    @JvmStatic
    fun getPassword(context: Context, username: String): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString("pwd_$username", null) ?: return null

        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainTextBytes = cipher.doFinal(cipherText)
            String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt password for user: $username", e)
            null
        }
    }

    /**
     * Clears any saved credentials for the given username.
     */
    @JvmStatic
    fun clearPassword(context: Context, username: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("pwd_$username").apply()
    }

    /**
     * Resets the secret key cache for testing.
     */
    @androidx.annotation.VisibleForTesting
    fun resetKeyForTesting() {
        fallbackKey = null
    }
}
