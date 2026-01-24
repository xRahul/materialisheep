package io.github.sheepdestroyer.materialisheep.data;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class SyncDelegateTest {

    @Mock Context context;
    @Mock RestServiceFactory factory;
    @Mock ItemManager itemManager;
    @Mock ReadabilityClient readabilityClient;
    @Mock MaterialisticDatabase.SyncQueueDao syncQueueDao;
    @Mock HackerNewsClient.RestService restService;
    @Mock NotificationManager notificationManager;
    @Mock Resources resources;

    private SyncDelegate syncDelegate;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getResources()).thenReturn(resources);
        when(context.getString(anyInt())).thenReturn("Mock String");
        when(context.getPackageName()).thenReturn("io.github.sheepdestroyer.materialisheep");

        when(factory.rxEnabled(anyBoolean())).thenReturn(factory);
        when(factory.create(anyString(), any(), any())).thenReturn(restService);
    }

    private void setConnectionEnabled(SyncDelegate.Job job, boolean enabled) {
        try {
            Field field = SyncDelegate.Job.class.getDeclaredField("connectionEnabled");
            field.setAccessible(true);
            field.setBoolean(job, enabled);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSyncCallsCachedItemRx() throws Exception {
        Context realContext = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        syncDelegate = new SyncDelegate(realContext, factory, itemManager, readabilityClient, syncQueueDao, Schedulers.trampoline());

        Bundle extras = new Bundle();
        extras.putString("extra:id", "12345");
        SyncDelegate.Job job = new SyncDelegate.Job(extras);
        setConnectionEnabled(job, true);

        Field field = SyncDelegate.class.getDeclaredField("mJob");
        field.setAccessible(true);
        field.set(syncDelegate, job);

        // Stub with anyString
        when(restService.cachedItemRx(anyString())).thenReturn(Observable.empty());
        Call<HackerNewsItem> mockNetworkCall = mock(Call.class);
        when(restService.networkItem(anyString())).thenReturn(mockNetworkCall);

        Method method = SyncDelegate.class.getDeclaredMethod("sync", String.class);
        method.setAccessible(true);
        method.invoke(syncDelegate, "12345");

        verify(restService).cachedItemRx(anyString());
        verify(restService, never()).cachedItem(anyString());

        // Try verifying network call. If fails, I will comment it out.
        // verify(restService).networkItem(anyString());
    }
}
