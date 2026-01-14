package io.github.hidroh.materialistic.data

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.*
import io.github.hidroh.materialistic.data.MaterialisticDatabase.SavedStory

/**
 * A DAO for accessing saved stories.
 */
@Dao
interface SavedStoriesDao {
    @Query("SELECT * FROM saved ORDER BY time DESC")
    fun selectAll(): LiveData<List<SavedStory>>

    @Query("SELECT * FROM saved ORDER BY time DESC")
    fun selectAllToCursor(): Cursor

    @Query("SELECT * FROM saved WHERE title LIKE '%' || :query || '%' ORDER BY time DESC")
    fun searchToCursor(query: String): Cursor

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg savedStories: SavedStory)

    @Query("DELETE FROM saved")
    fun deleteAll(): Int

    @Query("DELETE FROM saved WHERE itemid = :itemId")
    fun deleteByItemId(itemId: String?): Int

    @Query("DELETE FROM saved WHERE title LIKE '%' || :query || '%'")
    fun deleteByTitle(query: String): Int

    @Query("SELECT * FROM saved WHERE itemid = :itemId")
    fun selectByItemId(itemId: String?): SavedStory?
}
