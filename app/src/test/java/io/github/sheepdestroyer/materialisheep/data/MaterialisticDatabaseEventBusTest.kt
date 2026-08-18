package io.github.sheepdestroyer.materialisheep.data

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MaterialisticDatabaseEventBusTest {

    private lateinit var database: MaterialisticDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MaterialisticDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSetLiveValue_threadSafeConcurrentEmissions() = runBlocking {
        val count = 50
        val threadPool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(count)
        val collectedUris = mutableListOf<Uri>()

        val collectJob = launch(Dispatchers.Default) {
            database.eventsFlow.take(count).toList(collectedUris)
        }

        // Give collector a brief moment to register
        Thread.sleep(50)

        for (i in 1..count) {
            val uri = Uri.parse("content://io.github.sheepdestroyer.materialisheep/item/$i")
            threadPool.execute {
                database.setLiveValue(uri)
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        collectJob.join()
        threadPool.shutdown()

        assertEquals(count, collectedUris.size)
    }

    @Test
    fun testSetLiveValue_nullUriIsSafelyIgnored() {
        // Should not throw or emit null
        database.setLiveValue(null)
        assertNotNull(database.eventsFlow)
        assertNotNull(database.liveData)
    }
}
