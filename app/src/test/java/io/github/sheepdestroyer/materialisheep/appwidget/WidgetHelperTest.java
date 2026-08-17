package io.github.sheepdestroyer.materialisheep.appwidget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppWidgetManager;

import io.github.sheepdestroyer.materialisheep.R;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {31, 34})
public class WidgetHelperTest {

    private Context context;
    private WidgetHelper widgetHelper;
    private ItemManager itemManager;
    private ItemManager searchManager;
    private AppWidgetManager appWidgetManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        itemManager = mock(ItemManager.class);
        searchManager = mock(ItemManager.class);
        appWidgetManager = AppWidgetManager.getInstance(context);
        widgetHelper = new WidgetHelper(context);
        widgetHelper.mItemManager = itemManager;
        widgetHelper.mSearchManager = searchManager;
    }

    @Test
    public void refresh_setsEmptyView() {
        ShadowAppWidgetManager shadowManager = shadowOf(appWidgetManager);
        int appWidgetId = shadowManager.createWidget(WidgetProvider.class, R.layout.appwidget);

        when(itemManager.getStories(any(), anyInt())).thenReturn(null);

        widgetHelper.refresh(appWidgetId);

        // getViewFor(id) returns the inflated view based on the last update
        View widgetView = shadowManager.getViewFor(appWidgetId);
        assertNotNull("Widget view should not be null", widgetView);

        ListView listView = widgetView.findViewById(android.R.id.list);
        assertNotNull("ListView should be present", listView);

        View emptyView = listView.getEmptyView();

        // Without the fix, this should be null.
        // With the fix, this should be the view with R.id.empty

        assertNotNull("ListView should have an empty view set", emptyView);
        assertEquals(R.id.empty, emptyView.getId());
    }

    @Test
    public void refresh_fetchesStaleItemsIndividually() {
        ShadowAppWidgetManager shadowManager = shadowOf(appWidgetManager);
        int appWidgetId = shadowManager.createWidget(WidgetProvider.class, R.layout.appwidget);

        // Setup mock items
        Item[] stories = new Item[10];
        for (int i = 0; i < 10; i++) {
            Item item = mock(Item.class);
            when(item.getId()).thenReturn(String.valueOf(i));
            when(item.getLocalRevision()).thenReturn(0); // Ensure it triggers the update logic
            stories[i] = item;
        }
        when(itemManager.getStories(any(), anyInt())).thenReturn(stories);
        when(itemManager.getItem(anyString(), anyInt())).thenReturn(null);

        widgetHelper.refresh(appWidgetId);

        // Stale items (localRevision <= 0) are fetched individually from the network
        verify(itemManager, org.mockito.Mockito.times(10)).getItem(anyString(), anyInt());
    }
}
