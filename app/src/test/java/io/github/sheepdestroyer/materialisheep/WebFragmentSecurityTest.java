package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;
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
public class WebFragmentSecurityTest {

    private ApplicationComponent mockComponent;
    private ItemManager mockItemManager;
    private PopupMenu mockPopupMenu;
    private ReadabilityClient mockReadabilityClient;
    private FileDownloader mockFileDownloader;
    private WebFragment fragment;

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

        // Setup fragment
        Bundle args = new Bundle();
        WebItem item = mock(WebItem.class);
        when(item.getUrl()).thenReturn("http://example.com");
        when(item.getId()).thenReturn("1");
        args.putParcelable(WebFragment.EXTRA_ITEM, item);

        fragment = new WebFragment();
        fragment.setArguments(args);

        Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get()
                .getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
    }

    @Test
    public void testLoadContent_DisablesJavaScript() {
        // Trigger loadContent which simulates Readability mode
        fragment.loadContent();

        // After fix: JS should be disabled.
        assertFalse("JavaScript should be disabled for local content", fragment.mWebView.getSettings().getJavaScriptEnabled());
    }

    @Test
    public void testLoadUrl_EnablesJavaScript() {
        // Trigger load which should call loadUrl for normal web pages
        fragment.load();

        assertTrue("JavaScript should be enabled for remote URLs", fragment.mWebView.getSettings().getJavaScriptEnabled());
    }
}
