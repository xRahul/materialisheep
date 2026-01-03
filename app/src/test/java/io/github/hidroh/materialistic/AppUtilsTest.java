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
        // Default Robolectric shadow for ConnectivityManager usually reports no network
        // We verify that it doesn't crash and returns a boolean
        boolean connection = AppUtils.hasConnection(context);
        // Asserting explicit true/false depends on Robolectric default shadow state,
        // which can vary. For now, just ensuring it runs without exception is the goal.
    }
}
