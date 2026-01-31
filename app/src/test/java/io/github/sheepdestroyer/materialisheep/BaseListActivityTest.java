package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import io.github.sheepdestroyer.materialisheep.data.FileDownloader;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.ReadabilityClient;
import io.github.sheepdestroyer.materialisheep.data.SessionManager;
import io.github.sheepdestroyer.materialisheep.data.WebItem;
import androidx.appcompat.widget.SearchView;
import android.view.MenuItem;

import io.github.sheepdestroyer.materialisheep.widget.PopupMenu;
import io.reactivex.rxjava3.core.Scheduler;

@RunWith(RobolectricTestRunner.class)
public class BaseListActivityTest {

    @Mock
    ApplicationComponent applicationComponent;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        MaterialisticApplication application = ApplicationProvider.getApplicationContext();
        application.applicationComponent = applicationComponent;

        org.mockito.Mockito.doAnswer(invocation -> {
            ListActivity activity = invocation.getArgument(0);

            ActionViewResolver mockResolver = mock(ActionViewResolver.class);
            when(mockResolver.getActionView(any(MenuItem.class))).thenReturn(mock(SearchView.class));
            activity.mActionViewResolver = mockResolver;

            activity.mPopupMenu = mock(PopupMenu.class);
            activity.mSessionManager = mock(SessionManager.class);
            activity.mCustomTabsDelegate = mock(CustomTabsDelegate.class);
            activity.mKeyDelegate = mock(KeyDelegate.class);
            return null;
        }).when(applicationComponent).inject(any(ListActivity.class));

        // Mock fragment injections to avoid crashes if they rely on injected fields
        org.mockito.Mockito.doAnswer(invocation -> {
            ListFragment fragment = invocation.getArgument(0);
            fragment.mHnItemManager = mock(ItemManager.class);
            fragment.mAlgoliaItemManager = mock(ItemManager.class);
            fragment.mPopularItemManager = mock(ItemManager.class);
            fragment.mIoThreadScheduler = mock(Scheduler.class);
            return null;
        }).when(applicationComponent).inject(any(ListFragment.class));

        org.mockito.Mockito.doAnswer(invocation -> {
            ItemFragment fragment = invocation.getArgument(0);
            fragment.mItemManager = mock(ItemManager.class);
            return null;
        }).when(applicationComponent).inject(any(ItemFragment.class));

        org.mockito.Mockito.doAnswer(invocation -> {
            WebFragment fragment = invocation.getArgument(0);
            fragment.mItemManager = mock(ItemManager.class);
            fragment.mPopupMenu = mock(PopupMenu.class);
            fragment.mReadabilityClient = mock(ReadabilityClient.class);
            fragment.mFileDownloader = mock(FileDownloader.class);
            return null;
        }).when(applicationComponent).inject(any(WebFragment.class));
    }

    @Test
    public void testGetScrollableList_Portrait() {
        try (ActivityScenario<ListActivity> scenario = ActivityScenario.launch(ListActivity.class)) {
            scenario.onActivity(activity -> {
                Scrollable scrollable = activity.getScrollableList();
                assertNotNull("scrollable is null", scrollable);
                assertTrue(scrollable instanceof Fragment);
                assertEquals(BaseListActivity.LIST_FRAGMENT_TAG, ((Fragment) scrollable).getTag());
            });
        }
    }

    @Test
    @Config(qualifiers = "w820dp-land")
    public void testGetScrollableList_Landscape_NoSelection() {
        try (ActivityScenario<ListActivity> scenario = ActivityScenario.launch(ListActivity.class)) {
            scenario.onActivity(activity -> {
                // Ensure multi-pane
                assertTrue("Should be in multi-pane mode", activity.isMultiPane());

                Scrollable scrollable = activity.getScrollableList();
                assertNotNull(scrollable);
                assertTrue(scrollable instanceof Fragment);
                assertEquals(BaseListActivity.LIST_FRAGMENT_TAG, ((Fragment) scrollable).getTag());
            });
        }
    }

    @Test
    @Config(qualifiers = "w820dp-land")
    public void testGetScrollableList_Landscape_WithSelection() {
        try (ActivityScenario<ListActivity> scenario = ActivityScenario.launch(ListActivity.class)) {
            scenario.onActivity(activity -> {
                // Ensure multi-pane
                assertTrue("Should be in multi-pane mode", activity.isMultiPane());

                WebItem mockItem = mock(WebItem.class);
                when(mockItem.getId()).thenReturn("1");
                when(mockItem.isStoryType()).thenReturn(true);
                when(mockItem.getUrl()).thenReturn("http://example.com");

                activity.onItemSelected(mockItem);

                // Allow fragments to transact
                ShadowLooper.idleMainLooper();

                Scrollable scrollable = activity.getScrollableList();
                assertNotNull(scrollable);
                assertTrue(scrollable instanceof Fragment);

                // Should NOT be the list fragment
                assertNotEquals(BaseListActivity.LIST_FRAGMENT_TAG, ((Fragment) scrollable).getTag());

                // It should be the item fragment or similar
                // We verify it's scrollable, which is checked by return type (implied cast)
            });
        }
    }
}
