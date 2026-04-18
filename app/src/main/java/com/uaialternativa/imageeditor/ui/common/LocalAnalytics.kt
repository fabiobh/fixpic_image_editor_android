package com.uaialternativa.imageeditor.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAnalytics = staticCompositionLocalOf<AnalyticsManager> {
    error("No AnalyticsManager provided")
}
