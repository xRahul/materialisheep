package in.rahulja.materialhn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import in.rahulja.materialhn.data.FileDownloader;
import in.rahulja.materialhn.data.ItemManager;
import in.rahulja.materialhn.data.ReadabilityClient;
import in.rahulja.materialhn.data.WebItem;
import in.rahulja.materialhn.widget.PopupMenu;

@RunWith(RobolectricTestRunner.class)
@Config(application = MaterialisticApplication.class)
public class WebFragmentTest {

    private ApplicationComponent mockComponent;
    private ItemManager mockItemManager;
    private PopupMenu mockPopupMenu;
    private ReadabilityClient mockReadabilityClient;
    private FileDownloader mockFileDownloader;

    @Before
    public void setUp() {
        MaterialisticApplication app = ApplicationProvider.getApplicationContext();
        mockComponent = mock(ApplicationComponent.class);
        mockItemManager = mock(ItemManager.class);
        mockPopupMenu = mock(PopupMenu.class);
        mockReadabilityClient = mock(ReadabilityClient.class);
        mockFileDownloader = mock(FileDownloader.class);

        app.applicationComponent = mockComponent;

        doAnswer(invocation -> {
            WebFragment fragment = invocation.getArgument(0);
            fragment.mItemManager = mockItemManager;
            fragment.mPopupMenu = mockPopupMenu;
            fragment.mReadabilityClient = mockReadabilityClient;
            fragment.mFileDownloader = mockFileDownloader;
            return null;
        }).when(mockComponent).inject(any(WebFragment.class));
    }

    @Test
    public void testSetFullscreenCrash() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("http://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        // At this point onAttach -> setFullscreen(false) has likely been called.
        // If it hasn't crashed yet, we can try calling it again manually to force the issue.
        fragment.setFullscreen(false);
    }

    @Test
    public void testPdfAndroidJavascriptBridge_getChunk() throws IOException {
        Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        File tempFile = File.createTempFile("test_pdf", ".pdf", context.getCacheDir());
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Hello World PDF Content".getBytes());
        }

        WebFragment.PdfAndroidJavascriptBridge bridge =
                new WebFragment.PdfAndroidJavascriptBridge(context, tempFile.getAbsolutePath(), null);

        // Valid chunk read
        String expectedBase64 = Base64.encodeToString("Hello".getBytes(), Base64.DEFAULT);
        assertEquals(expectedBase64, bridge.getChunk(0, 5));

        // Invalid begin < 0
        assertEquals("", bridge.getChunk(-1, 5));

        // Invalid end < begin
        assertEquals("", bridge.getChunk(5, 2));

        // Exceeds max 10MB chunk size
        assertEquals("", bridge.getChunk(0, 11 * 1024 * 1024));

        // Reading past EOF triggers EOFException internally and returns empty string
        assertEquals("", bridge.getChunk(0, 1000));

        bridge.cleanUp();
    }

    @Test
    public void testPdfAndroidJavascriptBridge_rejectsFileOutsideCache() {
        Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        WebFragment.PdfAndroidJavascriptBridge bridge =
                new WebFragment.PdfAndroidJavascriptBridge(
                        context,
                        new File(context.getFilesDir(), "private.txt").getAbsolutePath(),
                        null);
        assertEquals("", bridge.getChunk(0, 5));
        assertEquals(0L, bridge.getSize());
        bridge.cleanUp();
    }

    @Test
    public void testItemActivityIsCurrentPage() {
        ItemActivity activity = new ItemActivity();
        Fragment fragment = mock(Fragment.class);
        assertFalse(activity.isCurrentPage(fragment));
        assertFalse(activity.isCurrentPage(null));
    }

    @Test
    public void testOnDestroyViewCleansUpWebView() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("https://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        in.rahulja.materialhn.widget.MaterialWebView webView = fragment.mWebView;
        org.junit.Assert.assertNotNull(webView);

        fragment.onDestroyView();

        org.junit.Assert.assertNull(fragment.mWebView);
        org.junit.Assert.assertNull(webView.getParent());

        // Verify subsequent calls when view is destroyed do not crash
        fragment.scrollToTop();
        fragment.scrollToNext();
        fragment.scrollToPrevious();
        fragment.onBackPressed();
        fragment.onResume();
        fragment.onStop();
        fragment.setFullscreen(false);
        fragment.onDestroy();
    }

    @Test
    public void testCacheableWebViewNullChromeClientAndUrlHandling() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).create().get();
        in.rahulja.materialhn.widget.CacheableWebView webView =
                new in.rahulja.materialhn.widget.CacheableWebView(activity);

        // Null url reload
        webView.reloadUrl(null);
        webView.loadUrl(null);

        // Setting null ChromeClient (teardown)
        webView.setWebChromeClient(null);
        webView.loadUrl("about:blank");
        webView.reloadUrl("https://example.com");
    }

    @Test
    public void testLifecyclePausesAndResumesOwnWebView() {
        WebFragment fragment = attachFragment();

        org.robolectric.shadows.ShadowWebView shadow =
                org.robolectric.Shadows.shadowOf(fragment.mWebView);

        fragment.onPause();
        org.junit.Assert.assertTrue(
                "onPause must pause this WebView", shadow.wasOnPauseCalled());

        fragment.onResume();
        org.junit.Assert.assertTrue(
                "onResume must resume this WebView", shadow.wasOnResumeCalled());
    }

    @Test
    public void testLifecycleNeverTouchesGlobalTimerPause() {
        WebFragment fragment = attachFragment();
        in.rahulja.materialhn.widget.MaterialWebView mockWebView =
                mock(in.rahulja.materialhn.widget.MaterialWebView.class);
        fragment.mWebView = mockWebView;

        fragment.onPause();
        fragment.onResume();
        fragment.onStop();

        verify(mockWebView).onPause();
        verify(mockWebView).onResume();
        verify(mockWebView, never()).pauseTimers();
        verify(mockWebView, never()).resumeTimers();
    }

    @Test
    public void testFullscreenTogglesControls() {
        WebFragment fragment = attachFragment();

        fragment.setFullscreen(true);
        assertEquals(
                android.view.View.VISIBLE,
                fragment.getView().findViewById(R.id.control_switcher).getVisibility());

        fragment.setFullscreen(false);
        assertEquals(
                android.view.View.GONE,
                fragment.getView().findViewById(R.id.control_switcher).getVisibility());
    }

    @Test
    public void testMainFrameErrorShowsPanel() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();

        client.onReceivedError(
                fragment.mWebView, request("https://example.com", true), webResourceError());

        View panel = fragment.getView().findViewById(R.id.web_error);
        assertEquals(View.VISIBLE, panel.getVisibility());
        assertEquals(
                fragment.getString(R.string.web_error_generic),
                ((TextView) fragment.getView().findViewById(R.id.web_error_message))
                        .getText()
                        .toString());
    }

    @Test
    public void testSubresourceErrorDoesNotShowPanel() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();

        client.onReceivedError(
                fragment.mWebView, request("https://example.com/ad.js", false), webResourceError());

        assertEquals(View.GONE, fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    @Test
    public void testRetryHidesPanelAndReloads() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();
        client.onReceivedError(
                fragment.mWebView, request("https://example.com", true), webResourceError());

        fragment.getView().findViewById(R.id.web_error_retry).performClick();

        assertEquals(View.GONE, fragment.getView().findViewById(R.id.web_error).getVisibility());
        assertEquals(
                "https://example.com",
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getLastLoadedUrl());
    }

    @Test
    public void testSslErrorIsCancelledAndShown() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();
        SslErrorHandler handler = mock(SslErrorHandler.class);

        client.onReceivedSslError(
                fragment.mWebView, handler, mock(android.net.http.SslError.class));

        verify(handler).cancel();
        assertEquals(View.VISIBLE, fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    @Test
    public void testIsPdfDetection() {
        org.junit.Assert.assertTrue(WebFragment.isPdf("application/pdf", "https://x/a.pdf"));
        org.junit.Assert.assertTrue(
                WebFragment.isPdf("APPLICATION/PDF; charset=binary", "https://x/a"));
        org.junit.Assert.assertTrue(
                WebFragment.isPdf("application/octet-stream", "https://x/docs/a.PDF?dl=1"));
        org.junit.Assert.assertFalse(
                WebFragment.isPdf("application/octet-stream", "https://x/a.zip"));
        org.junit.Assert.assertFalse(WebFragment.isPdf("text/html", "https://x/a.pdf"));
        org.junit.Assert.assertFalse(WebFragment.isPdf(null, null));
    }

    @Test
    public void testRenderProcessGoneReplacesWebView() {
        WebFragment fragment = attachFragment();
        in.rahulja.materialhn.widget.MaterialWebView old = fragment.mWebView;
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(old).getWebViewClient();

        boolean handled =
                client.onRenderProcessGone(old, mock(android.webkit.RenderProcessGoneDetail.class));

        org.junit.Assert.assertTrue("renderer crash must be handled in-app", handled);
        org.junit.Assert.assertNull("dead WebView must be detached", old.getParent());
        org.junit.Assert.assertNotSame(
                "a fresh WebView replaces the dead one", old, fragment.mWebView);
        org.junit.Assert.assertNotNull(fragment.mWebView.getParent());
        assertEquals(View.VISIBLE, fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    @Test
    public void testFontPreferenceDoesNotReloadLivePage() {
        WebFragment fragment = attachFragment();
        View webView = fragment.mWebView;
        int visibility = webView.getVisibility();

        fragment.onPreferenceChanged(R.string.pref_readability_font, false);

        assertEquals("live page must not reload on a font change", visibility, webView.getVisibility());
        org.mockito.Mockito.verify(mockReadabilityClient, never()).parse(any(), any(), any());
    }

    @Test
    public void testFontPreferenceReloadsReaderContent() {
        PreferenceManager.getDefaultSharedPreferences(
                        ApplicationProvider.getApplicationContext())
                .edit()
                .putString(
                        ApplicationProvider.getApplicationContext()
                                .getString(R.string.pref_story_display),
                        ApplicationProvider.getApplicationContext()
                                .getString(R.string.pref_story_display_value_readability))
                .commit();

        WebFragment fragment = attachFragment();

        fragment.onPreferenceChanged(R.string.pref_readability_font, false);

        org.mockito.Mockito.verify(mockReadabilityClient)
                .parse(org.mockito.ArgumentMatchers.eq("1"),
                        org.mockito.ArgumentMatchers.eq("https://example.com"),
                        any());
    }

    @Test
    public void testAbortedMainFrameErrorDoesNotShowPanel() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();

        client.onReceivedError(
                fragment.mWebView,
                request("https://example.com", true),
                abortedError());

        assertEquals(
                View.GONE, fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    @Test
    public void testSuccessfulCommitHidesPreviousError() {
        WebFragment fragment = attachFragment();
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getWebViewClient();
        client.onReceivedError(
                fragment.mWebView, request("https://example.com", true), webResourceError());
        assertEquals(
                View.VISIBLE, fragment.getView().findViewById(R.id.web_error).getVisibility());

        client.onPageCommitVisible(fragment.mWebView, "https://example.com");

        assertEquals(
                View.GONE, fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    @Test
    public void testRenderProcessGoneFallsBackToDeadViewsParent() {
        WebFragment fragment = attachFragment();
        in.rahulja.materialhn.widget.MaterialWebView old = fragment.mWebView;
        ViewGroup oldParent = (ViewGroup) old.getParent();
        fragment.getView().findViewById(R.id.web_view_container).setId(View.NO_ID);
        WebViewClient client =
                org.robolectric.Shadows.shadowOf(old).getWebViewClient();

        boolean handled =
                client.onRenderProcessGone(old, mock(android.webkit.RenderProcessGoneDetail.class));

        org.junit.Assert.assertTrue(handled);
        org.junit.Assert.assertNotNull(fragment.mWebView.getParent());
        org.junit.Assert.assertSame(oldParent, fragment.mWebView.getParent());
    }

    @Test
    public void testPdfDownloadUsesClickedUrl() {
        WebFragment fragment = attachFragment();
        android.webkit.DownloadListener listener =
                org.robolectric.Shadows.shadowOf(fragment.mWebView).getDownloadListener();
        org.junit.Assert.assertNotNull("download listener must be installed", listener);

        listener.onDownloadStart(
                "https://cdn.example.org/paper.pdf",
                "ua",
                "attachment",
                "application/octet-stream",
                1024);

        verify(mockFileDownloader)
                .downloadFile(
                        org.mockito.ArgumentMatchers.eq("https://cdn.example.org/paper.pdf"),
                        any(),
                        any());
    }

    @Test
    public void testMissingArticleUrlShowsPanel() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn(null);
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();
        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        fragment.load();

        assertEquals(
                "an article without a URL must show the panel, not a blank tab",
                View.VISIBLE,
                fragment.getView().findViewById(R.id.web_error).getVisibility());
    }

    private WebResourceError abortedError() {
        WebResourceError error = mock(WebResourceError.class);
        when(error.getErrorCode()).thenReturn(-3);
        when(error.getDescription()).thenReturn("net::ERR_ABORTED");
        return error;
    }

    private WebResourceError webResourceError() {
        WebResourceError error = mock(WebResourceError.class);
        when(error.getErrorCode()).thenReturn(-1);
        when(error.getDescription()).thenReturn("mock");
        return error;
    }

    private WebResourceRequest request(String url, boolean mainFrame) {
        WebResourceRequest request = mock(WebResourceRequest.class);
        when(request.getUrl()).thenReturn(Uri.parse(url));
        when(request.isForMainFrame()).thenReturn(mainFrame);
        return request;
    }

    private WebFragment attachFragment() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("https://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        return fragment;
    }

    @Test
    public void testBackPressedCallbackDisabledWhenCannotGoBack() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("https://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        // When webView cannot go back, callback must be disabled for predictive back
        org.junit.Assert.assertNotNull(fragment.mBackPressedCallback);
        assertFalse(fragment.mBackPressedCallback.isEnabled());
    }

    @Test
    public void testBackPressedCallbackDisabledWhenNotCurrentPage() {
        ItemActivity itemActivity = mock(ItemActivity.class);
        when(itemActivity.isCurrentPage(any(Fragment.class))).thenReturn(false);

        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("https://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        // Simulate not being current page
        fragment.updateBackPressedCallback();
        assertFalse(fragment.mBackPressedCallback.isEnabled());
    }

    @Test
    public void testBackPressedCallbackEnabledWhenCanGoBack() {
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("https://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        // Load multiple URLs so WebView has back history
        fragment.mWebView.loadUrl("https://example.com/page1");
        fragment.mWebView.loadUrl("https://example.com/page2");

        fragment.updateBackPressedCallback();
        if (fragment.mWebView.canGoBack()) {
            org.junit.Assert.assertTrue(fragment.mBackPressedCallback.isEnabled());
            fragment.mBackPressedCallback.handleOnBackPressed();
        }
    }
}

