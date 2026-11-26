package in.rahulja.materialhn.widget;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.net.Uri;
import android.webkit.WebResourceRequest;
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

    clientOf(webView).onPageCommitVisible(webView, "https://www.a.com/");

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
  public void testPageStartedDoesNotRevealStaleContent() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    clientOf(webView).onPageStarted(webView, "https://a.com", null);

    assertEquals("old page must stay hidden until commit", INVISIBLE, webView.getVisibility());
  }

  @Test
  public void testGetNavigationIsUpgradedToHttps() {
    MaterialWebView webView = createWebView();

    boolean handled =
        clientOf(webView).shouldOverrideUrlLoading(webView, request("http://a.com/x", "GET"));

    assertTrue(handled);
    assertEquals("https://a.com/x", Shadows.shadowOf(webView).getLastLoadedUrl());
  }

  @Test
  public void testPostNavigationIsNotRewritten() {
    MaterialWebView webView = createWebView();

    boolean handled =
        clientOf(webView).shouldOverrideUrlLoading(webView, request("http://a.com/x", "POST"));

    assertFalse("a POST body must not be dropped by the https rewrite", handled);
    assertNull(Shadows.shadowOf(webView).getLastLoadedUrl());
  }

  private WebResourceRequest request(String url, String method) {
    WebResourceRequest request = mock(WebResourceRequest.class);
    when(request.getUrl()).thenReturn(Uri.parse(url));
    when(request.getMethod()).thenReturn(method);
    when(request.isForMainFrame()).thenReturn(true);
    return request;
  }

  @Test
  public void testDirectLoadDoesNotUseBlankRoundTrip() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    assertEquals("https://a.com", Shadows.shadowOf(webView).getLastLoadedUrl());
  }
}
