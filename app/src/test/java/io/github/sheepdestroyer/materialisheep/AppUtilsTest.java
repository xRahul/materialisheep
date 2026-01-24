package io.github.sheepdestroyer.materialisheep;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
