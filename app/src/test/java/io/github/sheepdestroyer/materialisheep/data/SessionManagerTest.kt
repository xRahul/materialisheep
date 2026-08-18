package io.github.sheepdestroyer.materialisheep.data

import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

class SessionManagerTest {

    @Mock
    lateinit var localCache: LocalCache

    private val testDispatcher = Dispatchers.Unconfined
    private val testScope = CoroutineScope(testDispatcher)
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sessionManager = SessionManager(testDispatcher, localCache, testScope)
    }

    @Test
    fun isViewed_lazilyCallsCache() {
        val itemId = "1"
        val observable = sessionManager.isViewed(itemId)

        // Verify that cache is NOT accessed immediately
        verifyNoInteractions(localCache)

        observable.subscribe()

        // Verify that cache is accessed after subscription
        verify(localCache, times(1)).isViewed(itemId)
    }

    @Test
    fun isViewedList_lazilyCallsCache() {
        val itemIds = listOf("1", "2")
        val observable = sessionManager.isViewed(itemIds)

        // Verify that cache is NOT accessed immediately
        verifyNoInteractions(localCache)

        observable.subscribe()

        // Verify that cache is accessed after subscription
        verify(localCache, times(1)).isViewed(itemIds)
    }

    @Test
    fun view_launchesScopedCoroutine() = runBlocking {
        sessionManager.view("100")
        verify(localCache, times(1)).setViewed("100")
    }

    @Test
    fun view_nullOrEmptyDoesNothing() = runBlocking {
        sessionManager.view(null)
        sessionManager.view("")
        verifyNoInteractions(localCache)
    }

    @Test
    fun isItemViewed_suspendingCall() = runBlocking {
        `when`(localCache.isViewed("42")).thenReturn(true)
        assertTrue(sessionManager.isItemViewed("42"))
        assertFalse(sessionManager.isItemViewed(null))
        assertFalse(sessionManager.isItemViewed(""))
    }

    @Test
    fun areItemsViewed_suspendingCall() = runBlocking {
        `when`(localCache.isViewed(listOf("1", "2"))).thenReturn(listOf(true, false))
        val result = sessionManager.areItemsViewed(listOf("1", "2"))
        assertEquals(listOf(true, false), result)
        assertEquals(emptyList<Boolean>(), sessionManager.areItemsViewed(emptyList()))
    }

    @Test
    fun setViewed_suspendingCall() = runBlocking {
        sessionManager.setViewed("200")
        verify(localCache, times(1)).setViewed("200")
        sessionManager.setViewed(null)
        sessionManager.setViewed("")
    }
}
