package io.github.sheepdestroyer.materialisheep.data

import io.github.sheepdestroyer.materialisheep.accounts.UserServices
import io.github.sheepdestroyer.materialisheep.accounts.UserServicesClient
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.Protocol
import java.net.HttpURLConnection
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Verifies that the application clients are configured to use HTTPS URLs.
 *
 * Note: These tests verify the *application code's intent* (using https:// schemes).
 * The OS-level enforcement of HTTPS (blocking cleartext traffic) is handled by
 * app/src/main/res/xml/network_security_config.xml, which is applied by the Android framework.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkHttpsTest {

    @Mock
    lateinit var restServiceFactory: RestServiceFactory
    @Mock
    lateinit var sessionManager: SessionManager
    @Mock
    lateinit var favoriteManager: FavoriteManager
    @Mock
    lateinit var itemManager: ItemManager
    @Mock
    lateinit var callFactory: Call.Factory
    @Mock
    lateinit var call: Call

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(restServiceFactory.rxEnabled(ArgumentMatchers.anyBoolean())).thenReturn(restServiceFactory)

        // Stub for HN
        `when`(restServiceFactory.create(ArgumentMatchers.anyString(), ArgumentMatchers.eq(HackerNewsClient.RestService::class.java)))
            .thenReturn(mock(HackerNewsClient.RestService::class.java))

        // Stub for Algolia
        `when`(restServiceFactory.create(ArgumentMatchers.anyString(), ArgumentMatchers.eq(AlgoliaClient.RestService::class.java)))
            .thenReturn(mock(AlgoliaClient.RestService::class.java))

        `when`(callFactory.newCall(anyRequest())).thenReturn(call)

        // Stub call execution to return a real Response object
        val realResponse = Response.Builder()
            .request(Request.Builder().url("https://github.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(HttpURLConnection.HTTP_MOVED_TEMP)
            .message("Found")
            .build()

        `when`(call.execute()).thenReturn(realResponse)
    }

    private fun anyClass(): Class<*> {
        ArgumentMatchers.any(Class::class.java)
        return String::class.java
    }

    private fun anyRequest(): Request {
        ArgumentMatchers.any(Request::class.java)
        return Request.Builder().url("https://github.com").build()
    }

    private fun captureRequest(captor: ArgumentCaptor<Request>): Request {
        captor.capture()
        return Request.Builder().url("https://github.com").build()
    }

    @Test
    fun hackerNewsClient_usesHttps() {
        HackerNewsClient(restServiceFactory, sessionManager, favoriteManager)

        val urlCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(restServiceFactory).create(urlCaptor.capture(), ArgumentMatchers.eq(HackerNewsClient.RestService::class.java))

        assertTrue("HackerNewsClient URL should start with https", urlCaptor.value.startsWith("https://"))
    }

    @Test
    fun algoliaClient_usesHttps() {
        AlgoliaClient(restServiceFactory, itemManager, Schedulers.trampoline())

        val urlCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(restServiceFactory).create(urlCaptor.capture(), ArgumentMatchers.eq(AlgoliaClient.RestService::class.java))

        assertTrue("AlgoliaClient URL should start with https", urlCaptor.value.startsWith("https://"))
    }

    @Test
    fun userServicesClient_usesHttps() {
        val client = UserServicesClient(callFactory, Schedulers.trampoline())

        // Trigger a login call to capture the request URL
        client.login("user", "pass", false, mock(UserServices.Callback::class.java))

        val requestCaptor = ArgumentCaptor.forClass(Request::class.java)
        verify(callFactory).newCall(captureRequest(requestCaptor))

        val url = requestCaptor.value.url.toString()
        assertTrue("UserServicesClient login URL should start with https, was $url", url.startsWith("https://"))
    }
}
