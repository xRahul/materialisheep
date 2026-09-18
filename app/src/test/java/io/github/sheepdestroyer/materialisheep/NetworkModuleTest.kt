package io.github.sheepdestroyer.materialisheep

import io.github.sheepdestroyer.materialisheep.data.HackerNewsClient
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkModuleTest {

    private val interceptor = NetworkModule.CacheOverrideNetworkInterceptor()

    private fun createChain(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        statusCode: Int = 200
    ): Interceptor.Chain {
        val requestBuilder = Request.Builder().url(url)
        requestHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
        val request = requestBuilder.build()

        return object : Interceptor.Chain {
            override fun request(): Request = request

            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override val followRedirects: Boolean get() = false
            override val followSslRedirects: Boolean get() = false
            override val dns: okhttp3.Dns get() = okhttp3.Dns.SYSTEM
            override val socketFactory: javax.net.SocketFactory get() = javax.net.SocketFactory.getDefault()
            override val retryOnConnectionFailure: Boolean get() = true
            override val authenticator: okhttp3.Authenticator get() = okhttp3.Authenticator.NONE
            override val cookieJar: okhttp3.CookieJar get() = okhttp3.CookieJar.NO_COOKIES
            override val cache: okhttp3.Cache? get() = null
            override val proxy: java.net.Proxy? get() = null
            override val proxySelector: java.net.ProxySelector get() = java.net.ProxySelector.getDefault()
            override val proxyAuthenticator: okhttp3.Authenticator get() = okhttp3.Authenticator.NONE
            override val sslSocketFactoryOrNull: javax.net.ssl.SSLSocketFactory? get() = null
            override val x509TrustManagerOrNull: javax.net.ssl.X509TrustManager? get() = null
            override val hostnameVerifier: javax.net.ssl.HostnameVerifier get() = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
            override val certificatePinner: okhttp3.CertificatePinner get() = okhttp3.CertificatePinner.DEFAULT
            override val connectionPool: okhttp3.ConnectionPool get() = okhttp3.ConnectionPool()
            override val eventListener: okhttp3.EventListener get() = okhttp3.EventListener.NONE
            override fun connection(): okhttp3.Connection? = null
            override fun call(): okhttp3.Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 10000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 10000
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 10000
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun withDns(dns: okhttp3.Dns): Interceptor.Chain = this
            override fun withSocketFactory(socketFactory: javax.net.SocketFactory): Interceptor.Chain = this
            override fun withRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): Interceptor.Chain = this
            override fun withAuthenticator(authenticator: okhttp3.Authenticator): Interceptor.Chain = this
            override fun withCookieJar(cookieJar: okhttp3.CookieJar): Interceptor.Chain = this
            override fun withCache(cache: okhttp3.Cache?): Interceptor.Chain = this
            override fun withProxy(proxy: java.net.Proxy?): Interceptor.Chain = this
            override fun withProxySelector(proxySelector: java.net.ProxySelector): Interceptor.Chain = this
            override fun withProxyAuthenticator(proxyAuthenticator: okhttp3.Authenticator): Interceptor.Chain = this
            override fun withSslSocketFactory(sslSocketFactory: javax.net.ssl.SSLSocketFactory?, x509TrustManager: javax.net.ssl.X509TrustManager?): Interceptor.Chain = this
            override fun withHostnameVerifier(hostnameVerifier: javax.net.ssl.HostnameVerifier): Interceptor.Chain = this
            override fun withCertificatePinner(certificatePinner: okhttp3.CertificatePinner): Interceptor.Chain = this
            override fun withConnectionPool(connectionPool: okhttp3.ConnectionPool): Interceptor.Chain = this

        }
    }

    @Test
    fun testFeedEndpoint_shortCacheHeader() {
        val chain = createChain("https://${HackerNewsClient.HOST}/v0/topstories.json")
        val response = interceptor.intercept(chain)
        assertEquals("max-age=60", response.header("Cache-Control"))
    }

    @Test
    fun testItemEndpoint_longCacheHeader() {
        val chain = createChain("https://${HackerNewsClient.HOST}/v0/item/12345.json")
        val response = interceptor.intercept(chain)
        assertEquals("max-age=1800", response.header("Cache-Control"))
    }

    @Test
    fun testUserEndpoint_mediumCacheHeader() {
        val chain = createChain("https://${HackerNewsClient.HOST}/v0/user/someone.json")
        val response = interceptor.intercept(chain)
        assertEquals("max-age=300", response.header("Cache-Control"))
    }

    @Test
    fun testExplicitNoCache_preserved() {
        val chain = createChain(
            "https://${HackerNewsClient.HOST}/v0/topstories.json",
            mapOf("Cache-Control" to "no-cache")
        )
        val response = interceptor.intercept(chain)
        assertNull(response.header("Cache-Control"))
    }

    @Test
    fun testExternalHost_notOverridden() {
        val chain = createChain("https://example.com/api")
        val response = interceptor.intercept(chain)
        assertNull(response.header("Cache-Control"))
    }

    @Test
    fun testHttpError_notOverridden() {
        val chain = createChain("https://${HackerNewsClient.HOST}/v0/item/12345.json", statusCode = 500)
        val response = interceptor.intercept(chain)
        assertNull(response.header("Cache-Control"))
    }
}
