package io.github.sheepdestroyer.materialisheep.data

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class FavoriteManagerTest {

    @Mock
    lateinit var localCache: LocalCache
    @Mock
    lateinit var savedStoriesDao: SavedStoriesDao
    @Mock
    lateinit var context: Context
    @Mock
    lateinit var database: MaterialisticDatabase

    private lateinit var favoriteManager: FavoriteManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Override RxAndroid main thread scheduler
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        // Inject mock database into singleton
        setDatabaseInstance(database)

        // Mock database calls
        `when`(database.getSavedStoriesDao()).thenReturn(savedStoriesDao)
        `when`(database.getLiveData()).thenReturn(Mockito.mock(androidx.lifecycle.MutableLiveData::class.java) as androidx.lifecycle.MutableLiveData<Uri>)

        favoriteManager = FavoriteManager(localCache, Schedulers.trampoline(), savedStoriesDao)
    }

    @After
    fun tearDown() {
        // Reset singleton
        setDatabaseInstance(null)
        RxAndroidPlugins.reset()
    }

    @Test
    fun testRemoveMultiple_callsDeleteBatch() {
        val ids = listOf("1", "2", "3")

        favoriteManager.remove(context, ids)

        // Verify batch delete is called once
        verify(savedStoriesDao, times(1)).deleteByItemIds(ids)

        // Verify individual delete is NOT called
        verify(savedStoriesDao, never()).deleteByItemId(anyString())

        // Verify notifications are sent for each item
        verify(database, times(3)).setLiveValue(Mockito.any(Uri::class.java))
    }

    private fun setDatabaseInstance(instance: MaterialisticDatabase?) {
        try {
            val field: Field = MaterialisticDatabase::class.java.getDeclaredField("sInstance")
            field.isAccessible = true
            field.set(null, instance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
