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

            override fun connection(): okhttp3.Connection? = null
            override fun call(): okhttp3.Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 10000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 10000
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 10000
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
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
