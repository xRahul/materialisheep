package `in`.rahulja.materialhn

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class BackupRulesTest {

    private fun resource(name: String): String =
        java.io.File("src/main/res/xml/$name").readText()

    @Test
    fun `credentials and webview data are excluded from cloud backup and transfer`() {
        val rules = resource("data_extraction_rules.xml")
        assertTrue(rules.contains("""<exclude domain="sharedpref" path="auth_secure_store.xml" />"""))
        assertTrue(rules.contains("""<exclude domain="root" path="app_webview/" />"""))
        assertTrue(rules.contains("<cloud-backup>"))
        assertTrue(rules.contains("<device-transfer>"))
    }

    @Test
    fun `legacy backup rules exclude the same paths`() {
        val rules = resource("backup.xml")
        assertTrue(rules.contains("""<exclude domain="sharedpref" path="auth_secure_store.xml" />"""))
        assertTrue(rules.contains("""<exclude domain="root" path="app_webview/" />"""))
    }
}
