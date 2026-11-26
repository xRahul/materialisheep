package in.rahulja.materialhn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.webkit.WebView;
import org.junit.Before;
import org.junit.Test;

public class KeyDelegateWebViewHelperTest {

  private WebView mWebView;
  private KeyDelegate.WebViewHelper mHelper;

  @Before
  public void setUp() {
    mWebView = mock(WebView.class);
    mHelper = new KeyDelegate.WebViewHelper(mWebView);
  }

  @Test
  public void testScrollToTopPagesUp() {
    mHelper.scrollToTop();

    verify(mWebView).pageUp(true);
  }

  @Test
  public void testScrollToNextPagesDownWhenContentBelow() {
    when(mWebView.canScrollVertically(1)).thenReturn(true);
    when(mWebView.pageDown(false)).thenReturn(true);

    assertTrue(mHelper.scrollToNext());
    verify(mWebView).pageDown(false);
  }

  @Test
  public void testScrollToNextStopsAtBottom() {
    when(mWebView.canScrollVertically(1)).thenReturn(false);

    assertFalse(mHelper.scrollToNext());
    verify(mWebView, never()).pageDown(anyBoolean());
  }

  @Test
  public void testScrollToPreviousPagesUpWhenContentAbove() {
    when(mWebView.canScrollVertically(-1)).thenReturn(true);
    when(mWebView.pageUp(false)).thenReturn(true);

    assertTrue(mHelper.scrollToPrevious());
    verify(mWebView).pageUp(false);
  }

  @Test
  public void testScrollToPreviousStopsAtTop() {
    when(mWebView.canScrollVertically(-1)).thenReturn(false);

    assertFalse(mHelper.scrollToPrevious());
    verify(mWebView, never()).pageUp(anyBoolean());
  }
}
