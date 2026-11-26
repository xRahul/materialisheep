package in.rahulja.materialhn.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.webkit.WebSettings;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
