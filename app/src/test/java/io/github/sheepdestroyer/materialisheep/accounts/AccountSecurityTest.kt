package io.github.sheepdestroyer.materialisheep.accounts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountSecurityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AccountSecurity.clearPassword(context, "testuser")
        AccountSecurity.resetKeyForTesting()
    }

    @Test
    fun testSaveAndGetPassword_encryptedAndRetrieved() {
        val username = "testuser"
        val password = "SuperSecretPassword123!@#"

        AccountSecurity.savePassword(context, username, password)
        val retrieved = AccountSecurity.getPassword(context, username)

        assertEquals(password, retrieved)
    }

    @Test
    fun testClearPassword_removesPassword() {
        val username = "testuser"
        val password = "AnotherPassword456"

        AccountSecurity.savePassword(context, username, password)
        AccountSecurity.clearPassword(context, username)

        assertNull(AccountSecurity.getPassword(context, username))
    }

    @Test
    fun testSavePassword_nullOrEmptyClearsPassword() {
        val username = "testuser"
        AccountSecurity.savePassword(context, username, "existingPassword")
        AccountSecurity.savePassword(context, username, null)
        assertNull(AccountSecurity.getPassword(context, username))

        AccountSecurity.savePassword(context, username, "existingPassword2")
        AccountSecurity.savePassword(context, username, "")
        assertNull(AccountSecurity.getPassword(context, username))
    }

    @Test
    fun testCorruptedCiphertext_returnsNullGracefully() {
        val username = "testuser"
        val prefs = context.applicationContext.getSharedPreferences("auth_secure_store", Context.MODE_PRIVATE)
        prefs.edit().putString("pwd_$username", "corrupted_non_base64_data_!@#$").apply()

        assertNull(AccountSecurity.getPassword(context, username))
    }

    @Test
    fun testUnicodePassword_encryptedAndDecrypted() {
        val username = "testuser_unicode"
        val password = "🔑🔒Passwörd_日本語_12345"

        AccountSecurity.savePassword(context, username, password)
        assertEquals(password, AccountSecurity.getPassword(context, username))
    }

    @Test
    fun testNonExistentUser_returnsNull() {
        assertNull(AccountSecurity.getPassword(context, "unknown_user_999"))
    }
}
