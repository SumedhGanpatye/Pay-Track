package com.sumedh.moneytracker.domain.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class UserProfile(
    val username: String = ""
) {
    val isSignedUp: Boolean get() = username.isNotBlank()
}

class UserProfileStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun current(): UserProfile = _profile.value

    fun setUsername(raw: String): String? {
        val cleaned = raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(24)
        if (cleaned.isBlank()) return null
        prefs.edit { putString(KEY_USERNAME, cleaned) }
        _profile.value = UserProfile(username = cleaned)
        return cleaned
    }

    fun clear() {
        prefs.edit { remove(KEY_USERNAME) }
        _profile.value = UserProfile()
    }

    private fun read(): UserProfile {
        return UserProfile(
            username = prefs.getString(KEY_USERNAME, "").orEmpty().trim()
        )
    }

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_USERNAME = "username"

        fun personalizedGreeting(username: String, now: LocalTime = LocalTime.now()): String {
            val name = username.trim().ifBlank { "there" }
            val hour = now.hour
            val hello = when {
                hour in 5..11 -> "Good morning"
                hour in 12..16 -> "Good afternoon"
                hour in 17..20 -> "Good evening"
                else -> "Welcome back"
            }
            return "$hello, $name"
        }
    }
}
