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
}
