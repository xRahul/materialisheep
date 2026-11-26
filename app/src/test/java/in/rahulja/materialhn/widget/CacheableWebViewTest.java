package in.rahulja.materialhn.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.net.ConnectivityManager;
import android.webkit.WebSettings;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RunWith(RobolectricTestRunner.class)
public class CacheableWebViewTest {

  @Test
  public void testFileAndContentAccessDisabled() {
    Context context = ApplicationProvider.getApplicationContext();
    CacheableWebView webView = new CacheableWebView(context);

    assertFalse("File access should be disabled on CacheableWebView", webView.getSettings().getAllowFileAccess());
    assertFalse("Content access should be disabled on CacheableWebView", webView.getSettings().getAllowContentAccess());
  }

  @Test
  public void testOfflineArchiveReloadKeepsCacheOnlyMode() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    String url = "https://example.com/story";
    File archive = archiveFile(context, url);
    org.junit.Assert.assertTrue("test archive must exist", archive.createNewFile());

    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    Shadows.shadowOf(cm).setDefaultNetworkActive(false);
    org.junit.Assert.assertFalse("test must run offline",
        in.rahulja.materialhn.AppUtils.hasConnection(context));

    CacheableWebView webView = new CacheableWebView(context);
    webView.reloadUrl(url);

    assertEquals(
        "an offline archive must stay cache-only through the reload resolve",
        WebSettings.LOAD_CACHE_ONLY,
        webView.getSettings().getCacheMode());
  }

  private static File archiveFile(Context context, String url) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return new File(context.getCacheDir(), "webarchive-" + hex + ".mht");
  }

  @Test
  public void testCacheModeOnlineIsDefaultEvenWithArchive() {
    assertEquals(WebSettings.LOAD_DEFAULT, WebViewSettingsPolicy.cacheModeFor(true, false));
    assertEquals(WebSettings.LOAD_DEFAULT, WebViewSettingsPolicy.cacheModeFor(true, true));
  }

  @Test
  public void testCacheModeOfflineWithArchiveIsCacheOnly() {
    assertEquals(WebSettings.LOAD_CACHE_ONLY, WebViewSettingsPolicy.cacheModeFor(false, true));
  }

  @Test
  public void testCacheModeOfflineWithoutArchiveIsCacheElseNetwork() {
    assertEquals(WebSettings.LOAD_CACHE_ELSE_NETWORK, WebViewSettingsPolicy.cacheModeFor(false, false));
  }
}
