package io.github.hidroh.materialistic;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {
    @Test
    public void testHasConnection() {
        Context context = RuntimeEnvironment.getApplication();
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        org.robolectric.shadows.ShadowConnectivityManager shadowConnectivityManager = org.robolectric.Shadows
                .shadowOf(connectivityManager);

        // Test with no connection
        shadowConnectivityManager.setActiveNetworkInfo(null);
        assertFalse(AppUtils.hasConnection(context));

        // Test with connection
        shadowConnectivityManager.setActiveNetworkInfo(
                org.robolectric.shadows.ShadowNetworkInfo.newInstance(
                        android.net.NetworkInfo.DetailedState.CONNECTED,
                        android.net.ConnectivityManager.TYPE_WIFI, 0, true, true));
        assertTrue(AppUtils.hasConnection(context));
    }
}
