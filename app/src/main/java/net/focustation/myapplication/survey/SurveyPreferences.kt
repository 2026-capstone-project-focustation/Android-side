package net.focustation.myapplication.survey

import android.content.Context

object SurveyPreferences {
    private const val PREF_NAME = "focustation_survey_preferences"
    private const val KEY_COMPLETED = "survey_completed"
    private const val KEY_LATEST_ML_SCORE = "latest_ml_score"

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

    fun latestMlScore(context: Context): Double? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!preferences.contains(KEY_LATEST_ML_SCORE)) return null
        return Double.fromBits(preferences.getLong(KEY_LATEST_ML_SCORE, 0L))
    }

    fun setLatestMlScore(
        context: Context,
        score: Double?,
    ) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (score == null) {
                    remove(KEY_LATEST_ML_SCORE)
                } else {
                    putLong(KEY_LATEST_ML_SCORE, score.toRawBits())
                }
            }.apply()
    }
}
