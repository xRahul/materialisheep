package io.github.sheepdestroyer.materialisheep;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdBlockerTest {

    @Before
    public void setUp() throws Exception {
        // Reset AD_HOSTS to empty trie before each test
        Field field = AdBlocker.class.getDeclaredField("AD_HOSTS");
        field.setAccessible(true);
        field.set(null, new AdBlocker.TrieNode());
    }

    @Test
    public void testIsAd() throws Exception {
        // Setup mock hosts
        AdBlocker.TrieNode root = new AdBlocker.TrieNode();
        root.add("doubleclick.net");
        root.add("ad.service.com");

        // Inject hosts
        Field field = AdBlocker.class.getDeclaredField("AD_HOSTS");
        field.setAccessible(true);
        field.set(null, root);

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
}
