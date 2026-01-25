package io.github.sheepdestroyer.materialisheep.appwidget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Build;
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
import io.github.sheepdestroyer.materialisheep.data.ItemManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.S})
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
    }

    @Test
    public void refreshApi31_setsEmptyView() {
        ShadowAppWidgetManager shadowManager = shadowOf(appWidgetManager);
        int appWidgetId = shadowManager.createWidget(WidgetProvider.class, R.layout.appwidget);

        widgetHelper.refreshApi31(appWidgetId, itemManager, searchManager, null);

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
}
