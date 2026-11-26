package `in`.rahulja.materialhn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.rahulja.materialhn.data.MaterialisticDatabase.SyncQueueEntry

/**
 * A DAO for accessing the sync queue.
 */
@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(entry: SyncQueueEntry)

    @Query("SELECT itemid FROM sync_queue")
    fun getAll(): List<String>

    @Query("DELETE FROM sync_queue WHERE itemid = :itemId")
    fun delete(itemId: String)
}
