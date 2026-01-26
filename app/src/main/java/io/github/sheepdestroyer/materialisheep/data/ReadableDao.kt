package io.github.sheepdestroyer.materialisheep.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.sheepdestroyer.materialisheep.data.MaterialisticDatabase.Readable

/**
 * A DAO for accessing readable content.
 */
@Dao
interface ReadableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(readable: Readable)

    @Query("SELECT * FROM readable WHERE itemid = :itemId LIMIT 1")
    fun selectByItemId(itemId: String?): Readable?
}
