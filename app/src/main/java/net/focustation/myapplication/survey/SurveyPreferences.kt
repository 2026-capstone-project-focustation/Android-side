package net.focustation.myapplication.survey

import android.content.Context

object SurveyPreferences {
    private const val PREF_NAME = "focustation_survey_preferences"
    private const val KEY_COMPLETED = "survey_completed"

    fun isCompleted(context: Context): Boolean =
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    fun setCompleted(
        context: Context,
        completed: Boolean,
    ) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, completed)
            .apply()
    }
}
