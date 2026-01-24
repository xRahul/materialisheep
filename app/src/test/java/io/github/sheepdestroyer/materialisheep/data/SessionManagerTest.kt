package io.github.sheepdestroyer.materialisheep.data

import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

class SessionManagerTest {

    @Mock
    lateinit var localCache: LocalCache

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sessionManager = SessionManager(Schedulers.trampoline(), localCache)
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
}
