package com.rainyseason.cj.ticker.list

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.databinding.CoinTickerListFragmentBinding
import com.rainyseason.cj.tracking.logKeyParamsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
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
        askReviewTitle.text = "Tell us what's wrong : ("

        rightButton.text = "Submit"
        rightButton.setOnClickListener {
            moveToEndState()
            Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT)
                .show()
            tracker.logKeyParamsEvent(
                "app_review_negative_why",
                mapOf(
                    "content" to tellWhyEditText.text.toString()
                )
            )
        }

        leftButton.text = "Maybe later"
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

    if (BuildConfig.DEBUG) {
        showReview.isVisible = true
        showReview.setOnClickListener {
            moveToStartState()
        }
    }

    scope.launch {
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
                val askAgainInterval = 7L * 24 * 60 * 60 * 1000
                if (interval > askAgainInterval) {
                    withContext(Dispatchers.Main) {
                        moveToStartState()
                    }
                }
            }
        }
    }
}