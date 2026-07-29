package com.sumedh.moneytracker.data

import kotlinx.coroutines.flow.Flow

class NotificationDebugRepository(private val dao: ParsedNotificationDao) {

    fun observeRecent(limit: Int = 100): Flow<List<ParsedNotificationEntity>> =
        dao.observeRecent(limit)

    suspend fun log(entry: ParsedNotificationEntity) {
        dao.insert(entry)
        dao.trimToLast(100)
    }

    suspend fun getLatest(): ParsedNotificationEntity? = dao.getLatest()

    suspend fun clearAll() = dao.deleteAll()
}
