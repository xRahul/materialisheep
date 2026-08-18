package io.github.sheepdestroyer.materialisheep.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadableDaoTest {
    private lateinit var db: MaterialisticDatabase
    private lateinit var readableDao: ReadableDao
    private lateinit var savedStoriesDao: SavedStoriesDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaterialisticDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        readableDao = db.readableDao
        savedStoriesDao = db.savedStoriesDao
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPrune_retainsSavedStoriesAndLimitsUnsaved() {
        // Insert 10 unsaved items (1..10)
        for (i in 1..10) {
            readableDao.insert(MaterialisticDatabase.Readable(i.toString(), "content $i"))
        }

        // Save item "1" (the oldest item) in saved stories
        val savedStory = MaterialisticDatabase.SavedStory().apply {
            itemId = "1"
            url = "http://hn/1"
            title = "Title 1"
            time = "1000"
        }
        savedStoriesDao.insert(savedStory)

        // Prune keeping only the 3 newest items
        val deletedCount = readableDao.prune(limit = 3)

        // Out of 10 items:
        // Items 8, 9, 10 (newest 3) + Item 1 (saved) must remain.
        // Items 2..7 deleted (6 items).
        assertEquals(6, deletedCount)
        assertNotNull(readableDao.selectByItemId("1")) // preserved because it is saved
        assertNull(readableDao.selectByItemId("2"))   // pruned
        assertNull(readableDao.selectByItemId("7"))   // pruned
        assertNotNull(readableDao.selectByItemId("8")) // preserved by limit
        assertNotNull(readableDao.selectByItemId("9")) // preserved by limit
        assertNotNull(readableDao.selectByItemId("10")) // preserved by limit
    }
}
