package io.github.sheepdestroyer.materialisheep.data

import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

class FavoriteManagerLazyTest {

    @Mock
    lateinit var localCache: LocalCache
    @Mock
    lateinit var savedStoriesDao: SavedStoriesDao

    private lateinit var favoriteManager: FavoriteManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        favoriteManager = FavoriteManager(localCache, Schedulers.trampoline(), savedStoriesDao)
    }

    @Test
    fun check_lazilyCallsCache() {
        val itemId = "1"
        val observable = favoriteManager.check(itemId)

        // Verify that cache is NOT accessed immediately
        verifyNoInteractions(localCache)

        observable.subscribe()

        // Verify that cache is accessed after subscription
        verify(localCache, times(1)).isFavorite(itemId)
    }

    @Test
    fun checkList_lazilyCallsCache() {
        val itemIds = listOf("1", "2")
        val observable = favoriteManager.check(itemIds)

        // Verify that cache is NOT accessed immediately
        verifyNoInteractions(localCache)

        observable.subscribe()

        // Verify that cache is accessed after subscription
        verify(localCache, times(1)).isFavorite(itemIds)
    }
}
