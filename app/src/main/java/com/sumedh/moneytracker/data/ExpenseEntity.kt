package com.sumedh.moneytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val title: String,
    val category: String,
    val expenseType: String = ExpenseType.PERSONAL,
    val personName: String? = null,
    val groupName: String? = null,
    val date: String,
    val time: String,
    val source: String = ExpenseSource.MANUAL,
    val notes: String = "",
    val isReconciled: Boolean = false,
    val status: String = STATUS_COMPLETED
) {
    /** Display alias for older call sites that used [note]. */
    val note: String get() = title

    companion object {
        const val STATUS_COMPLETED = "completed"
        const val STATUS_PENDING = "pending"
    }
}

object ExpenseType {
    const val PERSONAL = "PERSONAL"
    const val SPLIT = "SPLIT"
}

object ExpenseSource {
    const val MANUAL = "MANUAL"
    const val NOTIFICATION = "NOTIFICATION"
}
