package io.github.sheepdestroyer.materialisheep;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AdBlockerTest {

    @Before
    public void setUp() throws Exception {
        // Reset AD_HOSTS to empty set before each test
        Field field = AdBlocker.class.getDeclaredField("AD_HOSTS");
        field.setAccessible(true);
        field.set(null, Collections.emptySet());
    }

    @Test
    public void testIsAd() throws Exception {
        // Setup mock hosts
        Set<String> testHosts = new HashSet<>();
        testHosts.add("doubleclick.net");
        testHosts.add("ad.service.com");

        // Inject hosts
        Field field = AdBlocker.class.getDeclaredField("AD_HOSTS");
        field.setAccessible(true);
        field.set(null, testHosts);

        // Test positive cases
        assertTrue(AdBlocker.isAd("http://doubleclick.net"));
        assertTrue(AdBlocker.isAd("http://g.doubleclick.net"));
        assertTrue(AdBlocker.isAd("https://ad.service.com/foo"));
        assertTrue(AdBlocker.isAd("https://sub.ad.service.com"));

        // Test negative cases
        assertFalse(AdBlocker.isAd("http://google.com"));
        assertFalse(AdBlocker.isAd("http://myservice.com"));

        // Test partial match that shouldn't match
        // "service.com" is a suffix of "ad.service.com" but not an ad host itself
        assertFalse(AdBlocker.isAd("http://service.com"));

        // Test non-dot host (should ignore)
        assertFalse(AdBlocker.isAd("http://localhost"));
    }
}
