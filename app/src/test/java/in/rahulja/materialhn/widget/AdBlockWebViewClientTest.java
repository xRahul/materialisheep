package in.rahulja.materialhn.widget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import androidx.test.core.app.ApplicationProvider;
import in.rahulja.materialhn.AdBlocker;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdBlockWebViewClientTest {

  // Present in assets/hagezi-pro-mini.txt.
  private static final String AD_URL = "https://ad.at.doubleclick.net/banner.js";
  private static final String CLEAN_URL = "https://news.example.org/story";
  private WebView mWebView;

  @Before
  public void setUp() {
    AdBlocker.resetForTesting();
    AdBlocker.init(ApplicationProvider.getApplicationContext(), Schedulers.trampoline());
    mWebView = mock(WebView.class);
  }

  @After
  public void tearDown() {
    AdBlocker.resetForTesting();
  }

  private WebResourceRequest request(String url, boolean mainFrame) {
    WebResourceRequest request = mock(WebResourceRequest.class);
    when(request.getUrl()).thenReturn(Uri.parse(url));
    when(request.isForMainFrame()).thenReturn(mainFrame);
    return request;
  }

  @Test
  public void mainFrameOnBlockedHostIsNeverBlocked() {
    AdBlockWebViewClient client = new AdBlockWebViewClient(true);

    assertNull(client.shouldInterceptRequest(mWebView, request(AD_URL, true)));
  }

  @Test
  public void subresourceOnBlockedHostIsBlocked() {
    AdBlockWebViewClient client = new AdBlockWebViewClient(true);

    assertNotNull(client.shouldInterceptRequest(mWebView, request(AD_URL, false)));
  }

  @Test
  public void disabledAdBlockPassesEverything() {
    AdBlockWebViewClient client = new AdBlockWebViewClient(false);

    assertNull(client.shouldInterceptRequest(mWebView, request(AD_URL, false)));
  }

  @Test
  public void subresourceOnCleanHostPasses() {
    AdBlockWebViewClient client = new AdBlockWebViewClient(true);

    assertNull(client.shouldInterceptRequest(mWebView, request(CLEAN_URL, false)));
  }
}
