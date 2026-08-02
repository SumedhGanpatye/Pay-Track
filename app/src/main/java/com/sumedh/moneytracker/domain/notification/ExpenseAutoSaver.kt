package com.sumedh.moneytracker.domain.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.data.ExpenseSource
import com.sumedh.moneytracker.data.ExpenseType
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Saves expenses from notification quick actions without opening the app.
 */
class ExpenseAutoSaver(
    private val context: Context,
    private val expenseRepository: ExpenseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val draftStore = PendingExpenseDraftStore(context)

    fun storeDraft(parsed: ParsedExpenseData): String {
        val draftId = UUID.randomUUID().toString()
        draftStore.save(
            draftId,
            PendingExpenseDraft(
                amount = parsed.amount,
                title = parsed.note?.takeIf { it.isNotBlank() } ?: parsed.title,
                personName = parsed.personName,
                groupName = parsed.groupName,
                notes = parsed.note.orEmpty()
            )
        )
        return draftId
    }

    fun saveFromDraft(draftId: String, category: String) {
        val draft = draftStore.consume(draftId) ?: return
        saveExpense(
            amount = draft.amount,
            title = draft.title,
            category = category,
            personName = draft.personName,
            groupName = draft.groupName,
            notes = draft.notes
        )
    }

    fun saveExpense(
        amount: Double,
        title: String,
        category: String,
        personName: String?,
        groupName: String?,
        notes: String = ""
    ) {
        scope.launch {
            val nowDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            expenseRepository.insert(
                ExpenseEntity(
                    amount = amount,
                    title = notes.takeIf { it.isNotBlank() } ?: title,
                    category = category,
                    expenseType = ExpenseType.SPLIT,
                    personName = personName,
                    groupName = groupName,
                    date = nowDate,
                    time = nowTime,
                    source = ExpenseSource.NOTIFICATION,
                    notes = notes
                )
            )
            ExpenseNotificationHelper.showExpenseAdded(
                context = context.applicationContext,
                amount = amount,
                category = category,
                note = notes.takeIf { it.isNotBlank() },
                requestor = personName
            )
        }
    }

    fun getDraft(draftId: String): PendingExpenseDraft? = draftStore.peek(draftId)
}

data class PendingExpenseDraft(
    val amount: Double,
    val title: String,
    val personName: String?,
    val groupName: String?,
    val notes: String = ""
)

private class PendingExpenseDraftStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(id: String, draft: PendingExpenseDraft) {
        prefs.edit {
            putString(key(id, "amount"), draft.amount.toString())
            putString(key(id, "title"), draft.title)
            putString(key(id, "person"), draft.personName)
            putString(key(id, "group"), draft.groupName)
            putString(key(id, "notes"), draft.notes)
        }
    }

    fun consume(id: String): PendingExpenseDraft? {
        val draft = peek(id) ?: return null
        prefs.edit {
            remove(key(id, "amount"))
            remove(key(id, "title"))
            remove(key(id, "person"))
            remove(key(id, "group"))
            remove(key(id, "notes"))
        }
        return draft
    }

    fun peek(id: String): PendingExpenseDraft? {
        val amount = prefs.getString(key(id, "amount"), null)?.toDoubleOrNull() ?: return null
        val title = prefs.getString(key(id, "title"), null) ?: return null
        return PendingExpenseDraft(
            amount = amount,
            title = title,
            personName = prefs.getString(key(id, "person"), null),
            groupName = prefs.getString(key(id, "group"), null),
            notes = prefs.getString(key(id, "notes"), null).orEmpty()
        )
    }

    private fun key(id: String, field: String) = "draft_${id}_$field"

    companion object {
        private const val PREFS = "pending_expense_drafts"
    }
}

object NotificationCategories {
    val quickActions = listOf(
        PaymentPrimaryCategories.FOOD,
        PaymentPrimaryCategories.TRAVEL,
        PaymentPrimaryCategories.GROCERY,
        PaymentPrimaryCategories.SHOPPING
    )
}
