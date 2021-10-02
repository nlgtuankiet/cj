package com.rainyseason.cj.ticker.list

import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.databinding.CoinTickerListFragmentBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.tracking.logKeyParamsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

fun CoinTickerListFragment.setupReview(
    binding: CoinTickerListFragmentBinding,
    scope: CoroutineScope,
) {
    val context = requireContext()
    val commonRepository = requireContext().coreComponent.commonRepository
    val reviewManager = ReviewManagerFactory.create(context)

    val reviewInfoRequest = scope.async {
        reviewManager.requestReview()
    }

    fun maybeShowGoogleInAppReview() {
        scope.launch {
            try {
                val reviewInfo = reviewInfoRequest.await()
                reviewManager.launchReview(requireActivity(), reviewInfo)
            } catch (ex: Exception) {
                context.coreComponent.firebaseCrashlytics.recordException(ex)
                if (BuildConfig.DEBUG) {
                    ex.printStackTrace()
                }
            }
        }
    }

    val askForReviewBackground = binding.askForReviewBackground
    val showReview = binding.showReview
    val askForReviewContainer = binding.askForReviewContainer
    val tellWhyEditText = binding.tellWhyEditText
    val tellWhy = binding.tellWhy
    val askReviewTitle = binding.askReviewTitle
    val leftButton = binding.leftButton
    val rightButton = binding.rightButton
    val tracker = rightButton.coreComponent.tracker
    val reviewIcon = binding.reviewIcon

    fun setUserLikeTheApp(value: Boolean) {
        scope.launch {
            commonRepository.setUserLikeTheApp(value)
        }
    }

    fun moveToEndState() {
        askForReviewBackground.isGone = true
        askForReviewContainer.isGone = true
        rightButton.dismissKeyboard()
    }

    fun moveToTellWhyState() {
        tellWhy.isVisible = true
        reviewIcon.isGone = true
        askReviewTitle.text = context.getString(R.string.inapp_review_tell_what_wrong)

        rightButton.text = context.getString(R.string.inapp_review_submit_feedback)
        rightButton.setOnClickListener {
            moveToEndState()
            Toast.makeText(context,
                R.string.inapp_review_thank_you_for_feedback,
                Toast.LENGTH_SHORT)
                .show()
            tracker.logKeyParamsEvent(
                "app_review_negative_why",
                mapOf(
                    "content" to tellWhyEditText.text.toString()
                )
            )
        }

        leftButton.text = context.getString(R.string.inapp_review_cancel_feedback)
        leftButton.setOnClickListener {
            moveToEndState()
        }
    }

    fun moveToStartState() {
        askForReviewBackground.isVisible = true
        askForReviewBackground.setOnClickListener { }

        leftButton.setOnClickListener {
            moveToTellWhyState()
            tracker.logKeyParamsEvent("app_review_negative")
            setUserLikeTheApp(false)
        }

        askForReviewContainer.isVisible = true

        rightButton.setOnClickListener {
            tracker.logKeyParamsEvent("app_review_positive")
            moveToEndState()
            maybeShowGoogleInAppReview()
            setUserLikeTheApp(true)
        }

    }

    if (DebugFlag.SHOW_TRIGGER_REVIEW_BUTTON.isEnable) {
        showReview.isVisible = true
        showReview.setOnClickListener {
            moveToStartState()
        }
    }

    scope.launch {
        val widgetUsed = commonRepository.getWidgetsUsed()
        val firstInstallTime = context.packageManager.getPackageInfo(context.packageName, 0)
            .firstInstallTime

        val timeUse = System.currentTimeMillis() - firstInstallTime
        val minTimeUse = TimeUnit.DAYS.toMillis(1)
        Timber.d("First install time is $firstInstallTime")
        if (widgetUsed < 2) {
            // wait for user use as least 1 widget before ask them for review
            return@launch
        }

        if (timeUse < minTimeUse) {
            // wait for user use as least 1 day before ask them for review
            return@launch
        }

        val isUserLikeTheApp = commonRepository.isUserLikeTheApp()
        if (isUserLikeTheApp) {
            maybeShowGoogleInAppReview()
            return@launch
        } else {
            val lastDislikeMilis = commonRepository.lastDislikeMilis()
            if (lastDislikeMilis == null) {
                withContext(Dispatchers.Main) {
                    moveToStartState()
                }
            } else {
                val interval = System.currentTimeMillis() - lastDislikeMilis
                val askAgainInterval = TimeUnit.DAYS.toMillis(7)
                if (interval > askAgainInterval) {
                    withContext(Dispatchers.Main) {
                        moveToStartState()
                    }
                }
            }
        }
    }
}