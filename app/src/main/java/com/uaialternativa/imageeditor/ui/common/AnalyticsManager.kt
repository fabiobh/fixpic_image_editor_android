package com.uaialternativa.imageeditor.ui.common

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logButtonClick(buttonId: String, screenName: String) {
        val bundle = Bundle().apply {
            putString("button_id", buttonId)
            putString("screen_name", screenName)
        }
        firebaseAnalytics.logEvent("button_click", bundle)
    }

    fun logDialogShow(dialogId: String) {
        val bundle = Bundle().apply {
            putString("dialog_id", dialogId)
        }
        firebaseAnalytics.logEvent("dialog_show", bundle)
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }
}
