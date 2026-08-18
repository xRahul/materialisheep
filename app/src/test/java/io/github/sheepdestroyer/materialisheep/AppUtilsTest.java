package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import androidx.core.util.Pair;
import io.github.sheepdestroyer.materialisheep.accounts.AccountSecurity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.text.format.DateUtils;
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
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {31, 34})
public class AppUtilsTest {

  @Test
  public void testUrlEquals() {
    // Exact identical URLs
    assertTrue(AppUtils.urlEquals("http://example.com", "http://example.com"));
    assertTrue(AppUtils.urlEquals("http://example.com/", "http://example.com/"));

    // Identical base URLs with different trailing slash presence
    assertTrue(AppUtils.urlEquals("http://example.com", "http://example.com/"));
    assertTrue(AppUtils.urlEquals("http://example.com/", "http://example.com"));

    // Different URLs
    assertFalse(AppUtils.urlEquals("http://example.com", "http://anotherexample.com"));
    assertFalse(AppUtils.urlEquals("http://example.com", "https://example.com"));

    // Case sensitivity
    assertFalse(AppUtils.urlEquals("http://example.com", "http://EXAMPLE.com"));

    // Edge cases: null and empty
    assertFalse(AppUtils.urlEquals(null, "http://example.com"));
    assertFalse(AppUtils.urlEquals("http://example.com", null));
    assertFalse(AppUtils.urlEquals(null, null));
    assertFalse(AppUtils.urlEquals("", "http://example.com"));
    assertFalse(AppUtils.urlEquals("http://example.com", ""));
    assertFalse(AppUtils.urlEquals("", ""));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testIsLowBattery() {
    Context context = ApplicationProvider.getApplicationContext();
    Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);

    // Not low battery
    intent.putExtra(android.os.BatteryManager.EXTRA_LEVEL, 50);
    intent.putExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
    intent.putExtra(
        android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_DISCHARGING);
    context.sendStickyBroadcast(intent);
    assertFalse(AppUtils.isLowBattery(context));

    // Low battery
    intent.putExtra(android.os.BatteryManager.EXTRA_LEVEL, 10);
    intent.putExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
    context.sendStickyBroadcast(intent);
    assertTrue(AppUtils.isLowBattery(context));

    // Charging (even if low)
    intent.putExtra(
        android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_CHARGING);
    context.sendStickyBroadcast(intent);
    assertFalse(AppUtils.isLowBattery(context));

    // Full
    intent.putExtra(
        android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_FULL);
    context.sendStickyBroadcast(intent);
    assertFalse(AppUtils.isLowBattery(context));
  }

  @Test
  public void testHasConnection() {
    Context context = ApplicationProvider.getApplicationContext();
    ConnectivityManager connectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    ShadowConnectivityManager shadowConnectivityManager = Shadows.shadowOf(connectivityManager);

    // Test with no connection
    shadowConnectivityManager.setDefaultNetworkActive(false);
    assertFalse(AppUtils.hasConnection(context));

    // Test with connection
    shadowConnectivityManager.setDefaultNetworkActive(true);
    android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
    if (activeNetwork != null) {
      android.net.NetworkCapabilities caps = new android.net.NetworkCapabilities();
      Shadows.shadowOf(caps)
          .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
      shadowConnectivityManager.setNetworkCapabilities(activeNetwork, caps);
    }
    assertTrue(AppUtils.hasConnection(context));

    // Test with disconnected network
    shadowConnectivityManager.setDefaultNetworkActive(false);
    assertFalse(AppUtils.hasConnection(context));
  }

  @Test
  public void testGetAbbreviatedTimeSpan() {
    long now = System.currentTimeMillis();

    // Test Years
    assertEquals("2y", AppUtils.getAbbreviatedTimeSpan(now - (2L * 365 * DateUtils.DAY_IN_MILLIS)));

    // Test Weeks
    assertEquals("3w", AppUtils.getAbbreviatedTimeSpan(now - (3 * DateUtils.WEEK_IN_MILLIS)));

    // Test Days
    assertEquals("4d", AppUtils.getAbbreviatedTimeSpan(now - (4 * DateUtils.DAY_IN_MILLIS)));

    // Test Hours
    assertEquals("5h", AppUtils.getAbbreviatedTimeSpan(now - (5 * DateUtils.HOUR_IN_MILLIS)));

    // Test Minutes
    assertEquals("10m", AppUtils.getAbbreviatedTimeSpan(now - (10 * DateUtils.MINUTE_IN_MILLIS)));

    // Test edge case (just now / 0 difference)
    assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(now));

    // Test edge case (future time)
    assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(now + DateUtils.DAY_IN_MILLIS));
  }

  @Test
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
  @SuppressWarnings("deprecation")
  public void testSystemUiHelperNew() {
    Window window = mock(Window.class);
    View decorView = mock(View.class);
    WindowInsetsController controller = mock(WindowInsetsController.class);

    when(window.getDecorView()).thenReturn(decorView);
    when(window.getInsetsController()).thenReturn(controller);
    when(controller.getSystemBarsBehavior())
        .thenReturn(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH);

    AppUtils.SystemUiHelper helper = new AppUtils.SystemUiHelper(window);

    helper.setFullscreen(true);
    verify(controller).hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
    verify(controller)
        .setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

    helper.setFullscreen(false);
    verify(controller).show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
    verify(controller).setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH);
  }

  @Test
  public void testOpenPlayStore_ActivityNotFound() {
    Context context = ApplicationProvider.getApplicationContext();
    Context wrapper =
        new ContextWrapper(context) {
          @Override
          public void startActivity(Intent intent) {
            throw new ActivityNotFoundException("Activity not found");
          }
        };

    AppUtils.openPlayStore(wrapper);
    assertEquals(context.getString(R.string.no_playstore), ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testGetCredentials_noUsername_returnsNull() {
    Context context = ApplicationProvider.getApplicationContext();
    Preferences.setUsername(context, null);
    assertNull(AppUtils.getCredentials(context));
  }

  @Test
  public void testGetCredentials_secureStorage_returnsCredentials() {
    Context context = ApplicationProvider.getApplicationContext();
    String username = "alice";
    String password = "alicePassword123";

    Preferences.setUsername(context, username);
    AccountManager accountManager = AccountManager.get(context);
    Account account = new Account(username, BuildConfig.APPLICATION_ID);
    accountManager.addAccountExplicitly(account, null, null);
    AccountSecurity.savePassword(context, username, password);

    Pair<String, String> credentials = AppUtils.getCredentials(context);
    assertNotNull(credentials);
    assertEquals(username, credentials.first);
    assertEquals(password, credentials.second);
  }

  @Test
  public void testGetCredentials_accountNotInAccountManager_returnsNull() {
    Context context = ApplicationProvider.getApplicationContext();
    String username = "ghost_user";
    Preferences.setUsername(context, username);
    AccountSecurity.savePassword(context, username, "some_pass");

    assertNull(AppUtils.getCredentials(context));
  }

  @Test
  public void testMakeSendIntentChooser() {
    Context context = ApplicationProvider.getApplicationContext();
    android.net.Uri testUri = android.net.Uri.parse("content://io.github.sheepdestroyer.materialisheep.fileprovider/saved/bookmarks.html");

    Intent chooser = AppUtils.makeSendIntentChooser(context, testUri);
    assertNotNull(chooser);
    assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
    assertEquals(context.getString(R.string.share_file), chooser.getStringExtra(Intent.EXTRA_TITLE));

    Intent target = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
    assertNotNull(target);
    assertEquals(Intent.ACTION_SEND_MULTIPLE, target.getAction());
    assertEquals("text/plain", target.getType());
    assertTrue((target.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);

    java.util.ArrayList<android.net.Uri> streams = target.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    assertNotNull(streams);
    assertEquals(1, streams.size());
    assertEquals(testUri, streams.get(0));

    android.content.ClipData clipData = target.getClipData();
    assertNotNull(clipData);
    assertEquals(1, clipData.getItemCount());
    assertEquals(testUri, clipData.getItemAt(0).getUri());
  }
}

