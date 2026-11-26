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

  @Test
  public void testPageFinishedRevealsWhenCommitNeverFires() {
    MaterialWebView webView = createWebView();
    webView.reloadHtml("<html><body>reader</body></html>");

    clientOf(webView).onPageFinished(webView, MaterialWebView.FILE);

    assertEquals(
        "data: pages must not stay hidden when commit is never reported",
        VISIBLE,
        webView.getVisibility());
  }

  @Test
  public void testStalePageFinishDoesNotRevealNewLoad() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://b.com");

    clientOf(webView).onPageFinished(webView, "https://a.com");

    assertEquals(
        "a superseded page must not flash while the new one loads",
        INVISIBLE,
        webView.getVisibility());
  }

  @Test
  public void testSameUrlReloadFinishDoesNotReveal() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    clientOf(webView).onPageFinished(webView, "https://a.com");

    assertEquals(
        "only commit may reveal a remote page",
        INVISIBLE,
        webView.getVisibility());
  }

  @Test
  public void testAbortedErrorKeepsLoadState() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com");

    android.webkit.WebResourceError aborted = mock(android.webkit.WebResourceError.class);
    when(aborted.getErrorCode()).thenReturn(-3);
    when(aborted.getDescription()).thenReturn("net::ERR_ABORTED");
    clientOf(webView).onReceivedError(webView, request("https://a.com", "GET"), aborted);

    assertTrue("an aborted load must not undo the newer load's history reset",
        webView.mClearHistoryOnCommit);
  }

  @Test
  public void testMainFrameErrorRestoresBackNavigation() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com/three");
    assertTrue("a fresh load hides back navigation until it commits",
        webView.mClearHistoryOnCommit);

    clientOf(webView).onReceivedError(webView, request("https://a.com/three", "GET"), error());

    assertFalse("a failed load must not keep back navigation disabled",
        webView.mClearHistoryOnCommit);
  }

  @Test
  public void testFailedLoadNeverRevealsItself() {
    MaterialWebView webView = createWebView();
    webView.reloadUrl("https://a.com/three");

    clientOf(webView).onReceivedError(webView, request("https://a.com/three", "GET"), error());
    clientOf(webView).onPageFinished(webView, "https://a.com/three");

    assertEquals(
        "a failed page must not replace the error panel",
        INVISIBLE,
        webView.getVisibility());
  }

  private android.webkit.WebResourceError error() {
    android.webkit.WebResourceError error = mock(android.webkit.WebResourceError.class);
    when(error.getErrorCode()).thenReturn(-2);
    when(error.getDescription()).thenReturn("net::ERR_NAME_NOT_RESOLVED");
    return error;
  }
}
