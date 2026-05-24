package net.focustation.myapplication.ui.screen.onboarding

import android.content.Context

class OnboardingStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences("focustation_onboarding", Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun userName(): String = preferences.getString(KEY_USER_NAME, "").orEmpty()

    fun saveUserName(name: String) {
        preferences.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun permissionsRequested(): Boolean = preferences.getBoolean(KEY_PERMISSIONS_REQUESTED, false)

    fun markPermissionsRequested() {
        preferences.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply()
    }

    private companion object {
        const val KEY_COMPLETED = "completed"
        const val KEY_USER_NAME = "user_name"
        const val KEY_PERMISSIONS_REQUESTED = "permissions_requested"
    }
}
