package com.sumedh.moneytracker

import android.app.Application
import com.sumedh.moneytracker.data.AppDatabase
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.profile.UserProfileStore
import com.sumedh.moneytracker.domain.upi.PaymentPreferences

class MoneyTrackerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao())
    }

    val paymentPreferences: PaymentPreferences by lazy {
        PaymentPreferences(this)
    }

    val customCategoryStore: CustomCategoryStore by lazy {
        CustomCategoryStore(this)
    }

    val userProfileStore: UserProfileStore by lazy {
        UserProfileStore(this)
    }
}
