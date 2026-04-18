package com.uaialternativa.imageeditor.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.play.core.review.ReviewManagerFactory
import com.uaialternativa.imageeditor.R

@Composable
fun SmartReviewDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    var currentStep by remember { mutableStateOf(ReviewStep.LIKE_APP) }

    when (currentStep) {
        ReviewStep.LIKE_APP -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(id = R.string.review_dialog_title)) },
                confirmButton = {
                    TextButton(onClick = {
                        analytics.logButtonClick("review_like_yes", "ReviewDialog")
                        launchInAppReview(context)
                        onDismiss()
                    }) {
                        Text(text = stringResource(id = R.string.review_dialog_positive))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        analytics.logButtonClick("review_like_no", "ReviewDialog")
                        currentStep = ReviewStep.FEEDBACK_PROMPT
                    }) {
                        Text(text = stringResource(id = R.string.review_dialog_negative))
                    }
                }
            )
        }
        ReviewStep.FEEDBACK_PROMPT -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(id = R.string.feedback_dialog_title)) },
                text = { Text(text = stringResource(id = R.string.feedback_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        analytics.logButtonClick("review_feedback_yes", "ReviewDialog")
                        sendFeedbackEmail(context)
                        onDismiss()
                    }) {
                        Text(text = stringResource(id = R.string.feedback_dialog_positive))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        analytics.logButtonClick("review_feedback_no", "ReviewDialog")
                        onDismiss()
                    }) {
                        Text(text = stringResource(id = R.string.feedback_dialog_negative))
                    }
                }
            )
        }
    }
}

private enum class ReviewStep {
    LIKE_APP,
    FEEDBACK_PROMPT
}

private fun launchInAppReview(context: Context) {
    val activity = context.findActivity() ?: return
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}

private fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun sendFeedbackEmail(context: Context) {
    val email = "uaialternativa@gmail.com"
    val subject = context.getString(R.string.support_email_subject)
    val body = context.getString(R.string.support_email_body)
    
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    
    try {
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } catch (e: Exception) {
        // Handle case where no email app is installed
    }
}
