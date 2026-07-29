package com.sumedh.moneytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC, id DESC")
    fun getAllSortedByDateDesc(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC, id DESC")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Query("UPDATE expenses SET isReconciled = :reconciled WHERE id = :id")
    suspend fun updateReconciled(id: Long, reconciled: Boolean)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("DELETE FROM expenses WHERE date = :isoDate")
    suspend fun deleteByDate(isoDate: String): Int

    @Query("DELETE FROM expenses WHERE date >= :startInclusive AND date <= :endInclusive")
    suspend fun deleteByDateRange(startInclusive: String, endInclusive: String): Int
}
