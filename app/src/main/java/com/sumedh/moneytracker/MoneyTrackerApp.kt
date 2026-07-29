package com.sumedh.moneytracker

import android.app.Application
import com.sumedh.moneytracker.data.AppDatabase
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.data.NotificationDebugRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.notification.ExpenseAutoSaver
import com.sumedh.moneytracker.domain.notification.NotificationRepository
import com.sumedh.moneytracker.domain.upi.UpiPreferences
import com.sumedh.moneytracker.domain.profile.ThemePreferences
import com.sumedh.moneytracker.domain.profile.UserProfileStore
import com.sumedh.moneytracker.service.ExpenseDetectedNotificationHelper

class MoneyTrackerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao())
    }

    val notificationDebugRepository: NotificationDebugRepository by lazy {
        NotificationDebugRepository(database.parsedNotificationDao())
    }

    val expenseAutoSaver: ExpenseAutoSaver by lazy {
        ExpenseAutoSaver(this, expenseRepository)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(this, notificationDebugRepository, expenseAutoSaver)
    }

    val customCategoryStore: CustomCategoryStore by lazy {
        CustomCategoryStore(this)
    }

    val userProfileStore: UserProfileStore by lazy {
        UserProfileStore(this)
    }

    val upiPreferences: UpiPreferences by lazy {
        UpiPreferences(this)
    }

    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        ExpenseDetectedNotificationHelper.ensureChannel(this)
    }
}
