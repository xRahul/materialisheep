package io.github.sheepdestroyer.materialisheep.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import io.github.sheepdestroyer.materialisheep.BuildConfig;
import io.github.sheepdestroyer.materialisheep.Preferences;
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class UserServicesClientTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword";

    @Mock
    private Call.Factory mCallFactory;
    @Mock
    private Call mCall;
    @Mock
    private UserServices.Callback mCallback;

    private Context mContext;
    private UserServicesClient mClient;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        RxAndroidPlugins.reset();
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());

        when(mCallFactory.newCall(any(Request.class))).thenReturn(mCall);

        setupCredentials(TEST_USERNAME, TEST_PASSWORD);

        mClient = new UserServicesClient(mCallFactory, Schedulers.trampoline());
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        AccountSecurity.clearPassword(mContext, TEST_USERNAME);
        Preferences.setUsername(mContext, null);
    }

    @Test
    public void testVoteUp_postsHowUpAndTriggersOnDoneTrueOn302() throws IOException {
        when(mCall.execute()).thenReturn(createResponse(HttpURLConnection.HTTP_MOVED_TEMP));

        boolean scheduled = mClient.voteUp(mContext, "12345", mCallback);
        assertTrue(scheduled);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mCallFactory).newCall(captor.capture());

        Request request = captor.getValue();
        assertEquals("https://news.ycombinator.com/vote", request.url().toString());
        assertTrue(request.body() instanceof FormBody);

        Map<String, String> formParams = extractFormParams((FormBody) request.body());
        assertEquals("12345", formParams.get("id"));
        assertEquals("up", formParams.get("how"));
        assertEquals(TEST_USERNAME, formParams.get("acct"));
        assertEquals(TEST_PASSWORD, formParams.get("pw"));

        verify(mCallback).onDone(true);
        verify(mCallback, never()).onError(any());
    }

    @Test
    public void testUnvote_postsHowUnAndTriggersOnDoneTrueOn302() throws IOException {
        when(mCall.execute()).thenReturn(createResponse(HttpURLConnection.HTTP_MOVED_TEMP));

        boolean scheduled = mClient.unvote(mContext, "67890", mCallback);
        assertTrue(scheduled);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mCallFactory).newCall(captor.capture());

        Request request = captor.getValue();
        assertEquals("https://news.ycombinator.com/vote", request.url().toString());
        assertTrue(request.body() instanceof FormBody);

        Map<String, String> formParams = extractFormParams((FormBody) request.body());
        assertEquals("67890", formParams.get("id"));
        assertEquals("un", formParams.get("how"));
        assertEquals(TEST_USERNAME, formParams.get("acct"));
        assertEquals(TEST_PASSWORD, formParams.get("pw"));

        verify(mCallback).onDone(true);
        verify(mCallback, never()).onError(any());
    }

    @Test
    public void testVoteUp_non302Response_triggersOnDoneFalse() throws IOException {
        when(mCall.execute()).thenReturn(createResponse(HttpURLConnection.HTTP_OK));

        boolean scheduled = mClient.voteUp(mContext, "12345", mCallback);
        assertTrue(scheduled);

        verify(mCallback).onDone(false);
        verify(mCallback, never()).onError(any());
    }

    @Test
    public void testUnvote_non302Response_triggersOnDoneFalse() throws IOException {
        when(mCall.execute()).thenReturn(createResponse(HttpURLConnection.HTTP_OK));

        boolean scheduled = mClient.unvote(mContext, "67890", mCallback);
        assertTrue(scheduled);

        verify(mCallback).onDone(false);
        verify(mCallback, never()).onError(any());
    }

    @Test
    public void testVoteUp_networkError_triggersOnError() throws IOException {
        IOException networkException = new IOException("Network timeout");
        when(mCall.execute()).thenThrow(networkException);

        boolean scheduled = mClient.voteUp(mContext, "12345", mCallback);
        assertTrue(scheduled);

        verify(mCallback).onError(networkException);
        verify(mCallback, never()).onDone(any(Boolean.class));
    }

    @Test
    public void testUnvote_networkError_triggersOnError() throws IOException {
        IOException networkException = new IOException("Network timeout");
        when(mCall.execute()).thenThrow(networkException);

        boolean scheduled = mClient.unvote(mContext, "67890", mCallback);
        assertTrue(scheduled);

        verify(mCallback).onError(networkException);
        verify(mCallback, never()).onDone(any(Boolean.class));
    }

    @Test
    public void testVoteUp_noCredentials_returnsFalse() {
        Preferences.setUsername(mContext, null);

        boolean scheduled = mClient.voteUp(mContext, "12345", mCallback);
        assertFalse(scheduled);

        verifyNoInteractions(mCallFactory);
        verifyNoInteractions(mCallback);
    }

    @Test
    public void testUnvote_noCredentials_returnsFalse() {
        Preferences.setUsername(mContext, null);

        boolean scheduled = mClient.unvote(mContext, "67890", mCallback);
        assertFalse(scheduled);

        verifyNoInteractions(mCallFactory);
        verifyNoInteractions(mCallback);
    }

    private void setupCredentials(String username, String password) {
        Preferences.setUsername(mContext, username);
        AccountManager accountManager = AccountManager.get(mContext);
        Account account = new Account(username, BuildConfig.APPLICATION_ID);
        accountManager.addAccountExplicitly(account, null, null);
        AccountSecurity.savePassword(mContext, username, password);
    }

    private Response createResponse(int code) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://news.ycombinator.com/vote").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Response " + code)
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
    }

    private Map<String, String> extractFormParams(FormBody formBody) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < formBody.size(); i++) {
            map.put(formBody.name(i), formBody.value(i));
        }
        return map;
    }
}
