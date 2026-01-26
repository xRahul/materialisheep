package io.github.sheepdestroyer.materialisheep;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testIsLowBattery() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);

        // Not low battery
        intent.putExtra(android.os.BatteryManager.EXTRA_LEVEL, 50);
        intent.putExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_DISCHARGING);
        context.sendStickyBroadcast(intent);
        assertFalse(AppUtils.isLowBattery(context));

        // Low battery
        intent.putExtra(android.os.BatteryManager.EXTRA_LEVEL, 10);
        intent.putExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
        context.sendStickyBroadcast(intent);
        assertTrue(AppUtils.isLowBattery(context));

        // Charging (even if low)
        intent.putExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_CHARGING);
        context.sendStickyBroadcast(intent);
        assertFalse(AppUtils.isLowBattery(context));

        // Full
        intent.putExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_FULL);
        context.sendStickyBroadcast(intent);
        assertFalse(AppUtils.isLowBattery(context));
    }

    @Test
    public void testHasConnection() {
        Context context = ApplicationProvider.getApplicationContext();
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        org.robolectric.shadows.ShadowConnectivityManager shadowConnectivityManager = org.robolectric.Shadows
                .shadowOf(connectivityManager);

        // Test with no connection
        shadowConnectivityManager.setDefaultNetworkActive(false);
        assertFalse(AppUtils.hasConnection(context));

        // Test with connection
        shadowConnectivityManager.setDefaultNetworkActive(true);
        android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            android.net.NetworkCapabilities caps = new android.net.NetworkCapabilities();
            org.robolectric.Shadows.shadowOf(caps)
                    .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            shadowConnectivityManager.setNetworkCapabilities(activeNetwork, caps);
        }
        assertTrue(AppUtils.hasConnection(context));

        // Test with disconnected network
        shadowConnectivityManager.setDefaultNetworkActive(false);
        assertFalse(AppUtils.hasConnection(context));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void testGetDisplayHeightNew() {
        Context context = mock(Context.class);
        WindowManager windowManager = mock(WindowManager.class);
        WindowMetrics windowMetrics = mock(WindowMetrics.class);
        Rect bounds = new Rect(0, 0, 1080, 2400);

        when(context.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
        when(windowManager.getCurrentWindowMetrics()).thenReturn(windowMetrics);
        when(windowMetrics.getBounds()).thenReturn(bounds);

        assertEquals(2400, AppUtils.getDisplayHeight(context));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SuppressWarnings("deprecation")
    public void testSystemUiHelperNew() {
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        WindowInsetsController controller = mock(WindowInsetsController.class);

        when(window.getDecorView()).thenReturn(decorView);
        when(window.getInsetsController()).thenReturn(controller);
        when(controller.getSystemBarsBehavior()).thenReturn(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH);

        AppUtils.SystemUiHelper helper = new AppUtils.SystemUiHelper(window);

        helper.setFullscreen(true);
        verify(controller).hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        verify(controller).setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        helper.setFullscreen(false);
        verify(controller).show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        verify(controller).setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH);
    }
}
