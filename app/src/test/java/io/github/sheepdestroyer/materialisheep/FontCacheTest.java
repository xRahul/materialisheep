package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.graphics.Typeface;
import androidx.test.core.app.ApplicationProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FontCacheTest {

    private Context context;
    private FontCache fontCache;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        fontCache = FontCache.getInstance();
    }

    @Test
    public void testGet_nullOrEmpty() {
        assertNull(fontCache.get(null, "DroidSans.ttf"));
        assertNull(fontCache.get(context, null));
        assertNull(fontCache.get(context, ""));
    }

    @Test
    public void testGet_invalidFontReturnsDefaultWithoutCrashing() {
        // Must not throw unchecked RuntimeException
        Typeface typeface = fontCache.get(context, "non_existent_corrupted_font.ttf");
        assertNotNull(typeface);
        assertEquals(Typeface.DEFAULT, typeface);
    }

    @Test
    public void testGet_validFontMappedResource() {
        Typeface tf1 = fontCache.get(context, "DroidSans.ttf");
        assertNotNull(tf1);

        Typeface tf2 = fontCache.get(context, "droid_sans");
        assertNotNull(tf2);
    }

    @Test
    public void testGet_concurrentAccess() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                try {
                    Typeface tf = fontCache.get(context, "RobotoSlab-Regular.ttf");
                    assertNotNull(tf);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertNull(error.get());
    }
}
