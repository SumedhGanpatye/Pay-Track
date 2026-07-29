package com.sumedh.moneytracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ParsedNotificationEntity): Long

    @Query("SELECT * FROM parsed_notifications ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<ParsedNotificationEntity>>

    @Query("SELECT * FROM parsed_notifications ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ParsedNotificationEntity?

    @Query("SELECT COUNT(*) FROM parsed_notifications")
    suspend fun count(): Int

    @Query("DELETE FROM parsed_notifications WHERE id NOT IN (SELECT id FROM parsed_notifications ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trimToLast(keep: Int = 100)

    @Query("DELETE FROM parsed_notifications")
    suspend fun deleteAll()
}
