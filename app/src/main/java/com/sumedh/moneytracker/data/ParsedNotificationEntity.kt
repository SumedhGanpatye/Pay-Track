package com.sumedh.moneytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_notifications")
data class ParsedNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val originalText: String,
    val notificationTitle: String = "",
    val extractedAmount: Double? = null,
    val extractedPerson: String? = null,
    val extractedGroup: String? = null,
    val parserUsed: String = "none",
    val packageName: String = "",
    val notificationKey: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
