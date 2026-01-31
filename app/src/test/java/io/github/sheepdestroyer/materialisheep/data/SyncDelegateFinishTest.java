package io.github.sheepdestroyer.materialisheep.data;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import io.reactivex.rxjava3.core.Observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@RunWith(RobolectricTestRunner.class)
public class SyncDelegateFinishTest {

    @Mock
    private RestServiceFactory restServiceFactory;
    @Mock
    private ItemManager itemManager;
    @Mock
    private ReadabilityClient readabilityClient;
    @Mock
    private SyncQueueDao syncQueueDao;
    @Mock
    private HackerNewsClient.RestService restService;
    @Mock
    private SyncDelegate.ProgressListener progressListener;

    private TestScheduler testScheduler;
    private SyncDelegate syncDelegate;
    private Context context;

    @Before
    public void setUp() {
        restServiceFactory = mock(RestServiceFactory.class);
        itemManager = mock(ItemManager.class);
        readabilityClient = mock(ReadabilityClient.class);
        syncQueueDao = mock(SyncQueueDao.class);
        restService = mock(HackerNewsClient.RestService.class);
        progressListener = mock(SyncDelegate.ProgressListener.class);

        context = ApplicationProvider.getApplicationContext();
        testScheduler = new TestScheduler();

        when(restServiceFactory.create(anyString(), eq(HackerNewsClient.RestService.class), any()))
                .thenReturn(restService);
        when(restServiceFactory.rxEnabled(true)).thenReturn(restServiceFactory);
        when(restService.cachedItemRx(anyString())).thenReturn(Observable.empty());

        syncDelegate = new SyncDelegate(context, restServiceFactory, itemManager, readabilityClient, syncQueueDao, testScheduler);
        syncDelegate.subscribe(progressListener);
    }

    @Test
    public void finishCalledOnlyOnce() {
        String jobId = "123";
        SyncDelegate.Job job = new SyncDelegate.Job(jobId);
        job.connectionEnabled = true;
        job.articleEnabled = true;
        job.commentsEnabled = false;
        job.readabilityEnabled = false;

        when(restService.cachedItemRx(jobId)).thenReturn(Observable.empty());
        when(restService.networkItemRx(jobId)).thenReturn(Observable.empty());

        syncDelegate.performSync(job);

        // Simulate item sync (this increments 'self' progress)
        HackerNewsItem item = mock(HackerNewsItem.class);
        when(item.getId()).thenReturn(jobId);
        when(item.getTitle()).thenReturn("Test Title");
        when(item.isStoryType()).thenReturn(true);
        when(item.getUrl()).thenReturn("http://example.com");

        syncDelegate.notifyItem(jobId, item);

        // Simulate article sync (this sets 'webProgress' to 100)
        // This should trigger finish() because max is met
        syncDelegate.notifyArticle(100);

        verify(progressListener, times(1)).onDone(jobId);

        // Call again to see if it triggers again
        syncDelegate.notifyArticle(100);

        verify(progressListener, times(1)).onDone(jobId);
    }
}
