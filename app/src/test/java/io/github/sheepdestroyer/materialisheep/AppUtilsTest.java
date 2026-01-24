package io.github.sheepdestroyer.materialisheep;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {
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
                        org.robolectric.Shadows.shadowOf(caps).addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
                        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, caps);
                }
                assertTrue(AppUtils.hasConnection(context));

                // Test with disconnected network
                shadowConnectivityManager.setDefaultNetworkActive(false);
                assertFalse(AppUtils.hasConnection(context));
        }

        @Test
        @Config(sdk = {Build.VERSION_CODES.R})
        public void testSystemUiHelper_api30() {
                Window window = mock(Window.class);
                View decorView = mock(View.class);
                when(window.getDecorView()).thenReturn(decorView);
                WindowInsetsController controller = mock(WindowInsetsController.class);
                when(window.getInsetsController()).thenReturn(controller);

                AppUtils.SystemUiHelper helper = new AppUtils.SystemUiHelper(window);
                helper.setFullscreen(true);
                verify(controller).hide(WindowInsets.Type.navigationBars());
                verify(controller).setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                helper.setFullscreen(false);
                verify(controller).show(WindowInsets.Type.navigationBars());
        }

        @Test
        @Config(sdk = {Build.VERSION_CODES.Q})
        public void testSystemUiHelper_legacy() {
                Window window = mock(Window.class);
                View decorView = mock(View.class);
                when(window.getDecorView()).thenReturn(decorView);
                when(decorView.getSystemUiVisibility()).thenReturn(0);

                AppUtils.SystemUiHelper helper = new AppUtils.SystemUiHelper(window);
                helper.setFullscreen(true);
                verify(decorView).setSystemUiVisibility(anyInt()); // Check flags
        }
}
