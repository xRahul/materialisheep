package io.github.sheepdestroyer.materialisheep.data;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
public class HackerNewsClientPerformanceTest {

    @Mock
    RestServiceFactory restServiceFactory;
    @Mock
    SessionManager sessionManager;
    @Mock
    FavoriteManager favoriteManager;
    @Mock
    HackerNewsClient.RestService restService;

    private HackerNewsClient client;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(restServiceFactory.rxEnabled(true)).thenReturn(restServiceFactory);
        when(restServiceFactory.create(anyString(), eq(HackerNewsClient.RestService.class))).thenReturn(restService);

        client = new HackerNewsClient(restServiceFactory, sessionManager, favoriteManager);
    }

    @Test
    public void testGetItemsPerformance() throws IOException {
        int delayMs = 100;
        int itemCount = 5;
        String[] ids = new String[itemCount];
        for (int i = 0; i < itemCount; i++) {
            ids[i] = String.valueOf(i);

            // Mock Observable for parallel getItem (optimized)
            final long id = i;
            when(restService.itemRx(String.valueOf(i))).thenReturn(
                Observable.timer(delayMs, TimeUnit.MILLISECONDS)
                    .map(t -> new HackerNewsItem(id))
            );
        }

        long startTime = System.currentTimeMillis();
        client.getItems(ids, ItemManager.MODE_DEFAULT);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Execution duration: " + duration + "ms");

        // Optimized: Should be close to delayMs (plus overhead), definitely less than sequential (500ms)
        // With parallel execution, it should be around 100ms + overhead.
        // We assert it is significantly faster than sequential.
        assertTrue("Duration should be less than " + (itemCount * delayMs * 0.8) + "ms but was " + duration,
                   duration < itemCount * delayMs * 0.8);

        // Also assert it is at least the delay of one item (sanity check)
        assertTrue("Duration should be at least " + delayMs + "ms but was " + duration,
                   duration >= delayMs);
    }
}
