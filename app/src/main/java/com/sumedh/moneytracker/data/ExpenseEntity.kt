package com.sumedh.moneytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val date: String,
    val time: String,
    val note: String,
    val category: String,
    val isReconciled: Boolean = false,
    /** completed | pending — future-ready for payment status. */
    val status: String = STATUS_COMPLETED
) {
    companion object {
        const val STATUS_COMPLETED = "completed"
        const val STATUS_PENDING = "pending"
    }
}
