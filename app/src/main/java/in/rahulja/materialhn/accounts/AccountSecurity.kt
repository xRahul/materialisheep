package `in`.rahulja.materialhn.accounts

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles hardware-backed Keystore AES-GCM encryption for user credentials.
 *
 * In release builds the key must come from the Android Keystore. If the Keystore
 * is unavailable or the key was invalidated, stored credentials are dropped and
 * the caller gets `null`, which forces a fresh login. Never fall back to an
 * ephemeral key in release: it would silently produce ciphertext that dies with
 * the process.
 */
object AccountSecurity {
    private const val TAG = "AccountSecurity"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "materialhn_master_auth_key"
    private const val PREFS_NAME = "auth_secure_store"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

    /** Test-only key, used when the Keystore is absent under Robolectric/JVM. */
    private var fallbackKey: SecretKey? = null

    private class KeystoreUnavailableException(cause: Throwable) :
        GeneralSecurityException("Android Keystore unavailable", cause)

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
        } catch (e: Exception) {
            if (!isTestEnvironment()) {
                throw KeystoreUnavailableException(e)
            }
            return fallbackKey ?: KeyGenerator.getInstance("AES").run {
                init(256)
                generateKey().also { fallbackKey = it }
            }
        }
    }

    /**
     * True only for the JVM/Robolectric test runner. A device build, debug or
     * release, must use the real Keystore: a throwaway key there would write
     * ciphertext that can never be read again.
     */
    private fun isTestEnvironment(): Boolean =
        android.os.Build.FINGERPRINT == "robolectric"

    /**
     * Encrypts and securely persists the password for the given username.
     * Returns false when the credential could not be stored securely.
     */
    @JvmStatic
    fun savePassword(context: Context, username: String, password: String?): Boolean {
        if (password.isNullOrEmpty()) {
            clearPassword(context, username)
            return true
        }

        return try {
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
            true
        } catch (e: KeystoreUnavailableException) {
            Log.e(TAG, "Keystore unavailable; refusing to store the password", e)
            clearPassword(context, username)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save password securely", e)
            clearPassword(context, username)
            false
        }
    }

    /**
     * Decrypts and returns the password for the given username, or null if it is
     * missing, corrupted, or encrypted with a key that no longer exists.
     */
    @JvmStatic
    fun getPassword(context: Context, username: String): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString("pwd_$username", null) ?: return null

        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) {
                clearPassword(context, username)
                return null
            }

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
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Keystore key invalidated; dropping stored password")
            clearPassword(context, username)
            null
        } catch (e: UnrecoverableKeyException) {
            Log.w(TAG, "Keystore key unrecoverable; dropping stored password")
            clearPassword(context, username)
            null
        } catch (e: KeystoreUnavailableException) {
            Log.e(TAG, "Keystore unavailable; dropping stored password")
            clearPassword(context, username)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt password for user: $username", e)
            clearPassword(context, username)
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
