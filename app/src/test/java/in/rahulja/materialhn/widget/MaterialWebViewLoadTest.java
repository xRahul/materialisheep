package in.rahulja.materialhn.widget;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.webkit.WebViewClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 31)
public class MaterialWebViewLoadTest {

  private MaterialWebView createWebView() {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    MaterialWebView webView = new MaterialWebView(activity);
    webView.setVisibility(INVISIBLE);
    return webView;
  }

  private WebViewClient clientOf(MaterialWebView webView) {
    ShadowWebView shadow = Shadows.shadowOf(webView);
    return shadow.getWebViewClient();
  }

  @Test
  public void testRedirectedUrlBecomesVisibleOnCommit() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    WebViewClient client = clientOf(webView);
    client.onPageStarted(webView, "https://www.a.com/", null);
    client.onPageCommitVisible(webView, "https://www.a.com/");

    assertEquals(VISIBLE, webView.getVisibility());
    assertFalse("back history is cleared for a fresh load", webView.canGoBack());
  }

  @Test
  public void testReloadHtmlBecomesVisibleOnCommit() {
    MaterialWebView webView = createWebView();
    webView.reloadHtml("<html><body>reader</body></html>");

    clientOf(webView).onPageCommitVisible(webView, MaterialWebView.FILE);

    assertEquals(VISIBLE, webView.getVisibility());
  }

  @Test
  public void testDirectLoadDoesNotUseBlankRoundTrip() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    assertEquals("https://a.com", Shadows.shadowOf(webView).getLastLoadedUrl());
  }
}
