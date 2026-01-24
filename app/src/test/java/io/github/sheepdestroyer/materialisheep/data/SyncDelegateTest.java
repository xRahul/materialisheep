package io.github.sheepdestroyer.materialisheep.data;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import retrofit2.Call;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SyncDelegateTest {

    @Mock
    private RestServiceFactory restServiceFactory;
    @Mock
    private ItemManager itemManager;
    @Mock
    private ReadabilityClient readabilityClient;
    @Mock
    private MaterialisticDatabase.SyncQueueDao syncQueueDao;
    @Mock
    private HackerNewsClient.RestService restService;

    private TestScheduler testScheduler;
    private SyncDelegate syncDelegate;
    private Context context;

    @Before
    public void setUp() {
        restServiceFactory = mock(RestServiceFactory.class);
        itemManager = mock(ItemManager.class);
        readabilityClient = mock(ReadabilityClient.class);
        syncQueueDao = mock(MaterialisticDatabase.SyncQueueDao.class);
        restService = mock(HackerNewsClient.RestService.class);

        context = ApplicationProvider.getApplicationContext();
        testScheduler = new TestScheduler();

        when(restServiceFactory.create(anyString(), eq(HackerNewsClient.RestService.class), any()))
                .thenReturn(restService);
        when(restServiceFactory.rxEnabled(true)).thenReturn(restServiceFactory);

        syncDelegate = new SyncDelegate(context, restServiceFactory, itemManager, readabilityClient, syncQueueDao, testScheduler);
    }

    @Test
    public void performSync_offloadsToIoScheduler() {
        String jobId = "123";
        SyncDelegate.Job job = new SyncDelegate.Job(jobId);
        job.connectionEnabled = true;

        when(restService.cachedItemRx(jobId)).thenReturn(Observable.just(new HackerNewsItem(123)));
        when(restService.networkItemRx(jobId)).thenReturn(Observable.just(new HackerNewsItem(123)));

        syncDelegate.performSync(job);

        // Trigger scheduler
        testScheduler.triggerActions();

        // Now it should be called
        verify(restService).cachedItemRx(jobId);
    }
}
