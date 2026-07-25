package com.sumedh.moneytracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun observeAllSortedByDateDesc(): Flow<List<ExpenseEntity>> =
        expenseDao.getAllSortedByDateDesc()

    suspend fun insert(expense: ExpenseEntity): Long =
        expenseDao.insert(expense)

    suspend fun update(expense: ExpenseEntity) =
        expenseDao.update(expense)

    suspend fun delete(expense: ExpenseEntity) =
        expenseDao.delete(expense)

    suspend fun reconcile(id: Long, reconciled: Boolean = true) =
        expenseDao.updateReconciled(id, reconciled)

    suspend fun getById(id: Long): ExpenseEntity? =
        expenseDao.getById(id)

    suspend fun deleteAll(): Int {
        expenseDao.deleteAll()
        return 1
    }

    suspend fun deleteByDate(isoDate: String): Int =
        expenseDao.deleteByDate(isoDate)

    suspend fun deleteByDateRange(startInclusive: String, endInclusive: String): Int =
        expenseDao.deleteByDateRange(startInclusive, endInclusive)

    suspend fun getAllOnce(): List<ExpenseEntity> =
        expenseDao.getAllOnce()
}
