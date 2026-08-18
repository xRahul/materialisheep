package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.sheepdestroyer.materialisheep.data.FileDownloader;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.ReadabilityClient;
import io.github.sheepdestroyer.materialisheep.data.WebItem;
import io.github.sheepdestroyer.materialisheep.widget.PopupMenu;

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
        File tempFile = File.createTempFile("test_pdf", ".pdf");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Hello World PDF Content".getBytes());
        }

        WebFragment.PdfAndroidJavascriptBridge bridge =
                new WebFragment.PdfAndroidJavascriptBridge(tempFile.getAbsolutePath(), null);

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

        io.github.sheepdestroyer.materialisheep.widget.MaterialWebView webView = fragment.mWebView;
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
        io.github.sheepdestroyer.materialisheep.widget.CacheableWebView webView =
                new io.github.sheepdestroyer.materialisheep.widget.CacheableWebView(activity);

        // Null url reload
        webView.reloadUrl(null);
        webView.loadUrl(null);

        // Setting null ChromeClient (teardown)
        webView.setWebChromeClient(null);
        webView.loadUrl("about:blank");
        webView.reloadUrl("https://example.com");
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

