package `in`.rahulja.materialhn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.rahulja.materialhn.data.MaterialisticDatabase.ReadStory

/**
 * A DAO for accessing read stories.
 */
@Dao
interface ReadStoriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(readStory: ReadStory)

    @Query("SELECT * FROM read WHERE itemid = :itemId LIMIT 1")
    fun selectByItemId(itemId: String?): ReadStory?

    @Query("SELECT * FROM read WHERE itemid IN (:itemIds)")
    fun selectByItemIds(itemIds: List<String>): List<ReadStory>
}
