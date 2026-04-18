package com.uaialternativa.imageeditor.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "review_prefs"
        private const val KEY_APP_OPEN_COUNT = "app_open_count"
        private const val KEY_SUCCESS_ACTION_COUNT = "success_action_count"
        
        private val REVIEW_MILESTONES = listOf(3, 7, 12)
    }

    fun incrementAppOpenCount() {
        val currentCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, currentCount + 1).apply()
    }

    fun incrementSuccessActionCount() {
        val currentCount = prefs.getInt(KEY_SUCCESS_ACTION_COUNT, 0)
        prefs.edit().putInt(KEY_SUCCESS_ACTION_COUNT, currentCount + 1).apply()
    }

    fun shouldShowReview(): Boolean {
        val openCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        val actionCount = prefs.getInt(KEY_SUCCESS_ACTION_COUNT, 0)
        
        return openCount in REVIEW_MILESTONES || actionCount in REVIEW_MILESTONES
    }
    
    fun getAppOpenCount(): Int = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
    fun getSuccessActionCount(): Int = prefs.getInt(KEY_SUCCESS_ACTION_COUNT, 0)
}
