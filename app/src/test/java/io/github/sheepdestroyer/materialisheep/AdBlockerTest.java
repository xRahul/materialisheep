package io.github.sheepdestroyer.materialisheep;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AdBlockerTest {

    @Before
    public void setUp() {
        AdBlocker.resetForTesting();
    }

    @org.junit.After
    public void tearDown() {
        AdBlocker.resetForTesting();
    }

    @Test
    public void testIsAd() {
        // Setup mock hosts
        AdBlocker.TrieNode root = new AdBlocker.TrieNode();
        root.add("doubleclick.net");
        root.add("ad.service.com");

        // Inject hosts via test hook
        AdBlocker.setAdHostsForTesting(root);

        // Test positive cases
        assertTrue("http://doubleclick.net should be ad", AdBlocker.isAd("http://doubleclick.net"));
        assertTrue("http://g.doubleclick.net should be ad", AdBlocker.isAd("http://g.doubleclick.net"));
        assertTrue("https://ad.service.com/foo should be ad", AdBlocker.isAd("https://ad.service.com/foo"));
        assertTrue("https://sub.ad.service.com should be ad", AdBlocker.isAd("https://sub.ad.service.com"));

        // Test negative cases
        assertFalse("http://google.com should NOT be ad", AdBlocker.isAd("http://google.com"));
        assertFalse("http://myservice.com should NOT be ad", AdBlocker.isAd("http://myservice.com"));

        // Test partial match that shouldn't match
        // "service.com" is a suffix of "ad.service.com" but not an ad host itself
        assertFalse("http://service.com should NOT be ad", AdBlocker.isAd("http://service.com"));

        // Test non-dot host (should ignore)
        assertFalse("http://localhost should NOT be ad", AdBlocker.isAd("http://localhost"));
    }

    @Test
    public void testIsAdUri() {
        // Setup mock hosts
        AdBlocker.TrieNode root = new AdBlocker.TrieNode();
        root.add("doubleclick.net");
        root.add("ad.service.com");

        // Inject hosts via test hook
        AdBlocker.setAdHostsForTesting(root);

        // Test positive cases
        assertTrue("http://doubleclick.net should be ad", AdBlocker.isAd(Uri.parse("http://doubleclick.net")));
        assertTrue("http://g.doubleclick.net should be ad", AdBlocker.isAd(Uri.parse("http://g.doubleclick.net")));

        // Test negative cases
        assertFalse("http://google.com should NOT be ad", AdBlocker.isAd(Uri.parse("http://google.com")));
    }

    @Test
    public void testInitFromHaGeZiAssetAndMatch() {
        Context context = ApplicationProvider.getApplicationContext();
        AdBlocker.init(context, Schedulers.trampoline());

        // Verify known ad/tracker hosts in HaGeZi Pro Mini
        assertTrue("googleads.g.doubleclick.net should be blocked",
                AdBlocker.isAd("https://googleads.g.doubleclick.net/pagead/ads"));
        assertTrue("pagead2.googlesyndication.com should be blocked",
                AdBlocker.isAd("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"));
        assertTrue("analytics.google.com should be blocked",
                AdBlocker.isAd("https://analytics.google.com/analytics/web/"));
        assertTrue("criteo.com should be blocked",
                AdBlocker.isAd("https://criteo.com/delivery/ajs.php"));
        assertTrue("static.criteo.net should be blocked",
                AdBlocker.isAd("https://static.criteo.net/js/ld/ld.js"));
        assertTrue("scorecardresearch.com should be blocked",
                AdBlocker.isAd("https://b.scorecardresearch.com/beacon.js"));
        assertTrue("moatads.com should be blocked",
                AdBlocker.isAd("https://z.moatads.com/swf/p.js"));

        // Case insensitivity test
        assertTrue("Uppercase URL should be blocked",
                AdBlocker.isAd("HTTPS://CRITEO.COM/DELIVERY/AJS.PHP"));
        assertTrue("Uppercase subdomain URL should be blocked",
                AdBlocker.isAd("HTTPS://PAGEAD2.GOOGLEADSERVICES.COM.COM/TEST"));

        // Verify legitimate sites are NOT blocked
        assertFalse("news.ycombinator.com should NOT be blocked",
                AdBlocker.isAd("https://news.ycombinator.com/"));
        assertFalse("google.com should NOT be blocked",
                AdBlocker.isAd("https://google.com/search?q=test"));
        assertFalse("github.com should NOT be blocked",
                AdBlocker.isAd("https://github.com/sheepdestroyer/materialisheep"));
        assertFalse("android.com should NOT be blocked",
                AdBlocker.isAd("https://developer.android.com/reference"));
        assertFalse("wikipedia.org should NOT be blocked",
                AdBlocker.isAd("https://en.wikipedia.org/wiki/Main_Page"));

        // Edge cases
        assertFalse("Empty string should not be ad", AdBlocker.isAd(""));
        assertFalse("Null string should not be ad", AdBlocker.isAd((String) null));
        assertFalse("Null Uri should not be ad", AdBlocker.isAd((Uri) null));
        assertNotNull("Empty resource response should not be null", AdBlocker.createEmptyResource());
    }
}
